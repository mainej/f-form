(ns f-form.dom
  "Tools for connecting a form and its fields to the DOM."
  (:require [clojure.string :as string]
            [f-form.field :as field]))

(defn field-handlers
  "Props: augment handlers for focusing and changing a field.

  Returns `props` with new `on-focus`, `on-change` and `on-blur` handlers,
  customized for managing the lifecycle of the field at the given `field-path`.

  When the DOM element's value changes, the provided handlers will be called
  with three arguments:
  * the field path
  * a function which will change the value of the field, using
  [[f-form.field/change]], [[f-form.field/gain-focus]],
  or [[f-form.field/lose-focus]] as appropriate.
  * the DOM Event which triggered the change.

  It is the responsibility of the provided `on-change` to apply the function to
  the field and to use the path to situate the updated field in the form,
  usually with [[f-form.form/update-field-by-path]]:

  ``` clojure
  [:input (f-form.dom/input
            {:on-change (fn [path f _]
                          (f-form.form/update-field-by-path form path f))}
            a-field)]
  ```

  This same `on-change` handler usually works for `on-blur` and `on-focus`, so
  those are optional and default to `on-change`. You can provide custom
  functions to handle those situations separately.

  `get-value` (which is usually configured by using one of the higher-level
  helpers like [[input]]) extracts the input's value from the DOM Event. You can
  also provide `parse-value` which will further massage the extracted value.
  This can be used, for example, to parse a DOM string into a float.

  ``` clojure
  (defn parse-float [s]
    (let [n (js/parseFloat s)]
      (if (js/isNaN n) nil n)))

  [:input (f-form.dom/input
            {:on-change   update-field-by-path
             :parse-value parse-float
             :type        \"number\"}
            price-field)]
  ```

  NOTE: It seems as though we could call the handlers with an already updated
  field, since we could close over the whole field instead of just its path.
  However, the handlers are *not* called with an updated field. This is to avoid
  problems in iOS Safari, which triggers both `on-change` and `on-blur` when the
  browser automatically advances to the next input (e.g when the user clicks
  `Done`, or chooses from a `<select>`). On that browser this leads to
  `on-change` being lost, because immediately after it is triggered, `on-blur`
  triggers with the closed-over field and its outdated `:field/value`. This
  doesn't seem to happen in other browsers, either because they don't
  automatically advance inputs or they re-render before triggering `on-blur`,
  giving time to close over the updated `:field/value`. The solution to this
  problem is to pass around paths and commutative functions, so that the
  handlers can apply the updates sequentially to a single field, instead of
  replacing the field wholesale."
  [{:keys [on-focus on-change on-blur parse-value]
    :or   {parse-value identity}
    :as   props}
   field-path
   get-value]
  (let [on-focus (or on-focus on-change)
        on-blur  (or on-blur on-change)]
    (assoc (dissoc props :parse-value)
           :on-focus  (fn [e]
                        (on-focus field-path field/gain-focus e))
           :on-change (fn [e]
                        ;; get value before event or target cease to exist
                        (let [new-val (parse-value (get-value e))]
                          (on-change field-path #(field/change % new-val) e)))
           :on-blur   (fn [e]
                        (on-blur field-path field/lose-focus e)))))

(defn presence
  "The contents of the string `s`, or `nil` if it is blank."
  [s]
  (when-not (string/blank? s) s))

(defn target-value
  "The value of the target of the DOM Event `e`: `e.target.value`. Converts blank
  strings into `nil`."
  [e]
  (presence (.-value (.-target e))))

(defn target-checked
  "Whether the target of the DOM Event `e` is checked: `e.target.checked`"
  [e]
  (.-checked (.-target e)))

(defn input
  "Props: create the handlers and other props necessary for an <input> tag"
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path target-value)
         :value value))

(defn checkbox
  "Props: create the handlers and other props necessary for an <input
  type=\"checkbox\"> tag. The `:field/value` should be a boolean."
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path target-checked)
         :type "checkbox"
         :checked value))

(defn radio
  "Props: create the handlers and other props necessary for an <input
  type=\"radio\"> tag. The radio will be checked if the `:field/value` matches
  the `option-value`. Selecting the radio will `field/change` the field to the
  `option-value`."
  [props {:keys [field/path field/value]} option-value]
  (assoc (field-handlers props path (fn [_e] option-value))
         :type "radio"
         :checked (= value option-value)))

(defn select
  "Props: create the handlers and other props necessary for a <select> tag, as
  described here https://reactjs.org/docs/forms.html#the-select-tag. Expects
  `options`, a seq of valid options, and `option-value`, a function which
  converts a single option into the *unique* string that identifies the option.
  The string should be the same as the value on the corresponding <option> tag.
  If `options` is a seq of strings, `option-value` is optional.

  ```clojure
  (let [options      [:a :b :c]
        option-value name]
    [:select (f-form.dom/select {:on-change update-field}
                                field
                                {:options      options
                                 :option-value option-value})
      (for [option options]
        ^{:key option}
        [:option {:value (option-value option)} (clojure.string/capitalize (name option))])])
  ```"
  [props {:keys [field/path field/value]} {:keys [options option-value] :or {option-value identity}}]
  (let [options-by-value (zipmap (map option-value options) options)]
    (assoc (field-handlers (assoc props :parse-value options-by-value) path target-value)
           :value (if value (option-value value) ""))))

(defn plugin
  "Props: create the handlers and other props necessary for plugins which call
  `:on-change` with a complex value. Useful for integrating with external
  libraries, like react-datetime-picker, which yield Clojure or JS objects
  instead of strings. Also often used to build \"custom\" input components which
  aren't based on <input>, <textarea> or <select> tags.

  The `:field/value` must be something these custom components will understand,
  and the `:on-change` handler must deal with these more complex values."
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path identity)
         :value value))
