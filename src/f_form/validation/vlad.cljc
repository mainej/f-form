(ns f-form.validation.vlad
  "Tools for validating forms with https://github.com/logaan/vlad.

  As any suite of validation tools should, this uses
  [[f-form.validation/set-valid]]. That is the only integration point with the
  rest of `f-form`.

  If you wish to use this namespace, you must provide `vlad` in your own
  dependencies. If you want to validate with another library, do not load this
  namespace."
  (:require [f-form.validation :as validation]
            [vlad.core :as vlad]))

(defn assign-names
  "See [[translate-errors]] for details about field-name assignment."
  [errors name-by-selector]
  (map (fn [e]
         (if (:name e)
           e
           (assoc e :name (let [name-or-submap (name-by-selector (:selector e))]
                            (if (map? name-or-submap)
                              (name-or-submap (:value-selector e))
                              name-or-submap)))))
       errors))

(defn validate
  "Similar to `vlad.core/field-errors`, but customized for dealing with forms:

  Takes a `form` and a `validation` and saves any errors on the fields.
  Updates the whole form's `:form/fields-valid?`.

  A field's errors are saved at `:field/errors` and its warnings (see
  [[warning]]) at `:field/warnings`.

  Errors and warnings are vectors of English strings, translated using the
  `field-names`.

  The `field-names` are tailored to work with forms as well, in coordination
  with [[value]]. Suppose you are collecting a birthday, like so:

  ``` clojure
  (def form
    (form/init [(field/init [:person/birthday]
                            {:date/month nil
                             :date/day   nil})]))

  (def validation
    (vlad/attr [:person/birthday] (f-form.validation.vlad/value
                                    (vlad/chain
                                      (vlad/attr [:date/month] (vlad/present))
                                      (vlad/attr [:date/year] (vlad/present))))))
  ```

  The `field-names` should be a hashmap whose keys are field paths and values
  are strings.

  ``` clojure
  (def field-names
    {[:person/birthday] \"Birthday\"})
  ```

  Then validation will be translated like so:

  ``` clojure
  (f-form.validation.vlad/validate form validation field-names)
  ;; => {:form/fields        {:person/birthday {:field/errors [\"Birthday is required.\"] ,,,}
  ;;     :form/fields-valid? false}
  ```

  Alternatively, if the individual components of a complex-valued field need
  their own names, they can be provided like so:

  ```clojure

  (def field-names
    {[:person/birthday] {[:date/month] \"Birth month\"
                         [:date/year] \"Birth year\"}})

  (f-form.validation.vlad/validate form validation field-names)
  ;; => {:form/fields {:person/birthday {:field/errors [\"Birth month is required.\"]}
  ;;     :form/fields-valid? false}
  ```
  "
  [form validation field-names]
  (let [{errors   ::error
         warnings ::warning} (->> (vlad/validate validation (:form/fields form))
                                  (group-by #(get % :urgency ::error)))

        errors   (-> errors
                     (assign-names field-names)
                     (vlad/translate-errors vlad/english-translation))
        warnings (-> warnings
                     (assign-names field-names)
                     (vlad/translate-errors vlad/english-translation))]
    (reduce (fn [form path]
              (update form :form/fields
                      update-in path
                      (fn [field]
                        (-> field
                            (assoc :field/errors (get errors path))
                            (assoc :field/warnings (get warnings path))))))
            (validation/set-valid form (empty? errors))
            (:form/field-paths form))))

(defrecord Value [validation]
  vlad/Validation
  (validate [_ data]
    (map (fn [error]
           (-> error
               (assoc :value-selector (:selector error))
               (dissoc :selector)))
     (vlad/validate validation (get data :field/value)))))

(defn value
  "Runs a validation on the data found at the `:field/value` of the surrounding
  attr.

  Example:
  ```clojure
  (vlad/validate (vlad/attr [:name]
                            (f-form.validation.vlad/value
                              (vlad/attr [:person/first-name]
                                         (vlad/one-of #{\"Valid\" \"Values\"}))))
                 {:name {:field/value {:person/first-name \"Invalid\"}}})
  ;; => [{:type           :vlad.core/one-of
  ;;      :set            #{\"Values\" \"Valid\"}
  ;;      :selector       [:name]
  ;;      :value-selector [:person/first-name]}]
  ```

  Note that `:selector` is the `:field/path`, and `:value-selector` is the path
  within the `:field/value`. These two selectors are used by [[validate]] to
  customize the names of invalid fields."
  ([] (Value. vlad/valid))
  ([validation] (Value. validation)))

(defn field
  "Like `vlad/attr`, but for a field. Defines a `validation` to run on the
  `:field/value` of a field whose `:field/path` matches the given `field-path`."
  ([field-path]            (vlad/attr field-path (value)))
  ([field-path validation] (vlad/attr field-path (value validation))))

(defrecord Urgency [level validation]
  vlad/Validation
  (validate [_ data]
    (map (fn [error]
           (assoc error :urgency level))
         (vlad/validate validation data))))

(defn urgency
  "Sets the urgency for any errors generated by `validation` to `level`."
  [level validation]
  (Urgency. level validation))

(defn warning
  "Sets the urgency for any errors generated by `validation` to `::warning`."
  [validation]
  (urgency ::warning validation))

(defn not-pristine
  "Checks that the field is not pristine."
  ([]
   (not-pristine {}))
  ([error-data]
   (vlad/predicate :field/pristine?
                   (merge {:type ::not-pristine} error-data))))

(defn value-in
  "Checks that the value is over `low` and under `high`, inclusive of both. No
  checking is done that `low` is lower than `high`."
  ([low high]
   (value-in low high {}))
  ([low high error-data]
   (vlad/predicate #(or (< high %)
                        (> low %))
                   (merge {:type ::value-in :low low :high high} error-data))))

(defn non-nil
  "Checks that the value is not nil."
  ([]
   (non-nil {}))
  ([error-data]
   (vlad/predicate nil?
                   (merge {:type ::vlad/present} error-data))))

(defn is-uuid
  "Checks that the value is a uuid."
  ([]
   (is-uuid {}))
  ([error-data]
   (vlad/predicate (complement uuid?)
                   (merge {:type ::is-uuid} error-data))))

(defn is-inst
  "Checks that the value is an instant."
  ([]
   (is-inst {}))
  ([error-data]
   (vlad/predicate (complement inst?)
                   (merge {:type ::vlad/present} error-data))))

(defn- pos-number? [x]
  (and (number? x)
       (> x 0)))

(defn pos-number
  "Checks that the value is a positive number."
  ([]
   (pos-number {}))
  ([error-data]
   (vlad/predicate (complement pos-number?)
                   (merge {:type ::pos-number} error-data))))

(defmulti translate-urgency :urgency)
(defmethod translate-urgency :default [_] "must")
(defmethod translate-urgency ::warning [_] "should")

(defmethod vlad/english-translation ::not-pristine [{:keys [name] :as error}]
  (str name " " (translate-urgency error) " be changed."))

(defmethod vlad/english-translation ::value-in [{:keys [name low high] :as error}]
  (str name " " (translate-urgency error) " be between " low " and " high "."))

(defmethod vlad/english-translation ::is-uuid [{:keys [name] :as error}]
  (str name " " (translate-urgency error) " be an id."))

(defmethod vlad/english-translation ::pos-number [{:keys [name] :as error}]
  (str name " " (translate-urgency error) " be a positive number."))
