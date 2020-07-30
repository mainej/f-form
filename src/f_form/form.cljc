(ns f-form.form
  "A form corresponds to an html `<form>` tag. Or to be more precise,
  one `<form>` is built out of one or more f-form.forms.

  A form is a collection of related [[f-form.field]]s which will be validated
  together.")

(defn field-by-path
  "Returns the field in the `form` stored at the given `path`."
  [form path]
  (get-in (:form/fields form) path))

(defn value-by-path
  "Returns the `:field/value` of the field in the `form` at the given `path`."
  [form path]
  (:field/value (field-by-path form path)))

(defn- replace-field
  "The core function for setting a field on a form. Stores the `field` on the
  `form` at the field's path. Should not be called directly. Instead use
  [[add-field]] to add or set fields and [[update-field-by-path]] to modify
  them."
  [form {:keys [field/path] :as field}]
  (update form :form/fields assoc-in path field))

(defn update-field-by-path
  "Update a field at the provided `path`, as with [[clojure.core/update]]. If
  the field does not exist, the update is ignored."
  [form path f & args]
  (if-let [field (field-by-path form path)]
    (replace-field form (apply f field args))
    form))

(def ^:private set-conj (fnil conj #{}))

(defn add-field
  "Add a new `field` to a `form`."
  [form field]
  (-> form
      (replace-field field)
      (update :form/field-paths set-conj (:field/path field))))

(defn init
  "Create a form containing the given `fields`.

  The `base`, if provided, is the initial value of the form before it is given
  any fields, and can be a place to hold extra data. Avoid conflicts by using
  keys from a namespace besides `form`."
  ([] (init []))
  ([fields] (init {} fields))
  ([base fields]
   (reduce add-field base fields)))

;; borrowed, with gratitude, from re-frame.utils
(defn- dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure.
  The key thing is that 'm' remains identical? to istelf if the path was never present"
  [m [k & ks :as _keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn remove-field-by-path
  "Remove a field from the `form`."
  [form path]
  (-> form
      (update :form/fields dissoc-in path)
      (update :form/field-paths disj path)))

(defn submitting
  "Mark that the `form` is being submitted to an external service."
  [form]
  (assoc form :form/submitting? true))

(defn submitted
  "Mark that the `form` is done being submitted."
  [form]
  (dissoc form :form/submitting?))

(defn submitting?
  "Check whether the `form` is currently being submitted."
  [form]
  (:form/submitting? form))

(defn fields
  "A seq of the fields on the `form`."
  [form]
  (map (fn [path]
         (field-by-path form path))
       (:form/field-paths form)))

(defn changes
  "A nested hashmap containing only the fields with changes. The keypaths into
  the hashmap are the `:field/path`s."
  [form]
  (->> form
       fields
       (remove :field/pristine?)
       (reduce (fn [result {:keys [field/path field/value]}]
                 (assoc-in result path value))
               {})))
