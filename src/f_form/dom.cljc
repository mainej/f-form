(ns f-form.dom
  "Tools for connecting a [[f-form.field]] to the DOM.

  The main utilities are the higher-level helpers [[input]], [[select]],
  [[checkbox]], and [[radio]]. Use [[plugin]] to integrate with other libraries."
  (:require [clojure.string :as string]
            [f-form.field :as field]))

(defn field-handlers
  "_Props:_ augment handlers for focusing and changing a field.

  NOTE: generally you won't use this function. Prefer the higher-level helpers
  [[input]], [[select]], [[checkbox]], [[radio]], and [[plugin]].

  Modifies the `:on-focus`, `:on-change` and `:on-blur` handlers in the provided
  `props`, customizing them to manage the lifecycle of the field at the given
  `field-path`. Any other map entries in `props` will be passed through
  unchanged (except for `:parse-value`, see below).

  When the DOM element's events fire, the provided handlers will be called with
  three arguments:

  * the field path
  * a function which will update the field (including changing its
    `:field/value` if relevant.)
  * the DOM Event which triggered the change.

  It is the responsibility of the provided handlers to apply the function to the
  field and to use the path to situate the updated field in the form, usually
  with [[f-form.form/update-field-by-path]]:

  ``` clojure
  [:input (f-form.dom/input
            {:on-change (fn [path f _]
                          (swap! form f-form.form/update-field-by-path path f))}
            a-field)]
  ```

  All three handlers usually work the same, so `:on-blur` and `:on-focus` are
  optional and default to `:on-change`.

  `get-value` (which is usually configured by using one of the higher-level
  helpers like [[input]]) extracts the `<input>`'s value from the DOM Event. You
  can also provide `:parse-value` in the `props` which will further massage the
  extracted value. This can be used, for example, to parse a DOM string into a
  float.

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
  field, since we could close over the whole updated field instead of just its
  path. However, we do *not* do this to avoid problems in iOS Safari, which
  triggers both `on-change` and `on-blur` when the browser automatically
  advances to the next input (e.g when the user clicks `Done`, or chooses from a
  `<select>`). On that browser this leads to `on-change` being lost, because the
  browser simultaneously triggers `on-blur` with the closed-over field and its
  outdated `:field/value`. This doesn't seem to happen in other browsers, either
  because they don't automatically advance inputs or they re-render before
  triggering `on-blur`, giving time to close over the updated `:field/value`.
  The solution to this problem is to pass around paths and commutative
  functions, so that the handlers can apply the updates sequentially to a single
  field, instead of replacing the field wholesale."
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

(defn ^:no-doc presence
  "_Helper:_ The contents of the string `s`, or `nil` if it is blank."
  [s]
  (when-not (string/blank? s) s))

(defn ^:no-doc target-value
  "_Helper:_ The value of the target of the DOM Event `e`: `e.target.value`. Converts blank
  strings into `nil`."
  [e]
  (presence (.-value (.-target e))))

(defn ^:no-doc target-checked
  "_Helper:_ Whether the target of the DOM Event `e` is checked: `e.target.checked`"
  [e]
  (.-checked (.-target e)))

(defn input
  "_Props:_ create the handlers and other props necessary for an `<input>` tag.
  `props` will be augmented as per [[field-handlers]].

  ``` clojure
  [:input (f-form.dom/input
            {:on-change update-field}
            street-field)]
  ```
  "
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path target-value)
         :value value))

(defn checkbox
  "_Props:_ create the handlers and other props necessary for an `<input
  type=\"checkbox\">` tag. The `:field/value` should be a boolean. `props` will
  be augmented as per [[field-handlers]].


  ``` clojure
  [:input (f-form.dom/checkbox
            {:on-change update-field}
            tos-field)]
  ```
  "
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path target-checked)
         :type "checkbox"
         :checked value))

(defn radio
  "_Props:_ create the handlers and other props necessary for an `<input
  type=\"radio\">` tag. The radio will be checked if the `:field/value` equals
  the `option-value`. Selecting the radio will [[f-form.field/change]] the field
  to the `option-value`. `props` will be augmented as per [[field-handlers]].

  ```clojure
  [:input (f-form.dom/radio
            {:name      \"titles\"
             :on-change update-field}
            title-field
            \"Dr.\")]
  ```
  "
  [props {:keys [field/path field/value]} option-value]
  (assoc (field-handlers props path (fn [_e] option-value))
         :type "radio"
         :checked (= value option-value)))

(defn select
  "_Props:_ create the handlers and other props necessary for a `<select>` tag,
  as described here https://reactjs.org/docs/forms.html#the-select-tag. `props`
  will be augmented as per [[field-handlers]]. Configure with `:options`, a seq
  of valid options, and `:option-value`, a function which returns a **string**
  that **uniquely** identifies an option. The string should be the same as the
  value on the corresponding `<option>` tag. If `:options` is a seq of strings,
  `:option-value` is optional.

  When an `<option>` is selected, the field will be changed to match the
  corresponding item in the `:options` (not the `<option>` value).

  Note that this helper does not generate the list of `<option>` tags; that is
  up to you. In particular, if you want a \"placeholder\" option, use `:value`
  `\"\"`.

  ```clojure
  (let [things [{:thing/id    \"a\"
                 :thing/label \"A\"}
                {:thing/id    \"b\"
                 :thing/label \"B\"}
                {:thing/id    \"c\"
                 :thing/label \"C\"}]]
    [:select (f-form.dom/select {:on-change update-field}
                                field
                                {:options      things
                                 :option-value :thing/id})
     [:option {:value \"\", :disabled true} \"Choose thing...\"]
     (for [{:keys [thing/id thing/label] :as thing} things]
       ^{:key id}
       ;; selecting this `<option>` will `f-form.field/change` the field to be
       ;; the `thing`.
       [:option {:value id} label])])
  ```"
  [props {:keys [field/path field/value]} {:keys [options option-value] :or {option-value identity}}]
  (let [options-by-value (zipmap (map option-value options) options)]
    (assoc (field-handlers (assoc props :parse-value options-by-value) path target-value)
           :value (if value (option-value value) ""))))

(defn plugin
  "_Props:_ create the handlers and other props necessary for plugins which call
  `:on-change` with a complex value. `props` will be augmented as per
  [[field-handlers]]. Useful for integrating with external libraries, like
  react-datetime-picker, which yield Clojure or JS objects instead of strings.
  Also often used to build \"custom\" input components which aren't based on
  `<input>`, `<textarea>` or `<select>` tags.

  The `:field/value` must be something these custom components will understand,
  and the `:on-change` handler must deal with these more complex values."
  [props {:keys [field/path field/value]}]
  (assoc (field-handlers props path identity)
         :value value))
