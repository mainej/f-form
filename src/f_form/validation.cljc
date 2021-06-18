(ns f-form.validation
  "Functions for setting or checking the validation on the form as a whole.

  A [[f-form.form]] is validated as a whole, and a summary is placed on the
  form. Field errors are placed on the fields themselves.

  f-form delegates validation to other validation libraries. See
  [[f-form.validation.vlad]] for more information about validating with
  [vlad](https://github.com/logaan/vlad).")

(defn set-valid
  "The integration point for validation libraries. Any external validation
  system should call this when a form becomes valid or invalid. See
  [[f-form.validation.vlad/validate]] for an example."
  [form valid?]
  (assoc form :form/fields-valid? valid?))

(defn valid?
  "Check whether the `form` has any validation errors."
  [form]
  (:form/fields-valid? form))

(def invalid?
  "Check whether the `form` has no validation errors."
  (complement valid?))
