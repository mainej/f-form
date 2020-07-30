(ns f-form.validation)

(defn set-valid [form valid?]
  (assoc form :form/fields-valid? valid?))

(defn valid?
  "Check whether the `form` has any validation errors. See
  [[f-form.validation.vlad]] for more information about validation."
  [form]
  (:form/fields-valid? form))

(def invalid?
  "Check whether the `form` has no validation errors."
  (complement valid?))
