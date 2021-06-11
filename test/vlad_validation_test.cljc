(ns vlad-validation-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [f-form.field :as field]
            [f-form.field-tracker :as tracker]
            [f-form.form :as form]
            [f-form.validation :as validation]
            [f-form.validation.vlad :as form.validation]
            [vlad.core :as vlad]))

(def person-form (form/init [(field/init [:name] {:person/first-name nil
                                                  :person/last-name  nil})
                             (field/init [:postal-code] "12345" tracker/full-tracker)
                             (field/init [:age] nil)]))

(def field-labels {;; Labeling of fields with simple paths:
                   [:postal-code] "Postal Code"
                   [:age]         "Age"
                   ;; Labeling of attrs inside a field:
                   [:name]        {[:person/first-name] "First Name"
                                   [:person/last-name]  "Last Name"}})

(t/deftest form-validation
  (let [validation (vlad/join
                    (form.validation/field [:name]
                                           (vlad/join
                                            (vlad/attr [:person/first-name]
                                                       (form.validation/non-nil))
                                            (vlad/attr [:person/last-name]
                                                       (form.validation/non-nil))))
                    (form.validation/field [:age]
                                           (vlad/chain
                                            (form.validation/pos-number)
                                            (form.validation/value-in 21 140))))
        form       (form.validation/validate person-form validation field-labels)]
    (t/testing "Summarizes at the form level"
      (t/is (validation/invalid? form)))
    (t/testing "Adds errors to each field"
      (t/is (= ["First Name is required." "Last Name is required."]
               (:field/errors (form/field-by-path form [:name]))))
      (t/is (= ["Age must be a positive number."]
               (:field/errors (form/field-by-path form [:age])))))
    (t/testing "Passes when form is complete"
      (t/is (-> person-form
                (form/update-field-by-path [:name] field/change {:person/first-name "John"
                                                                 :person/last-name  "Doe"})
                (form/update-field-by-path [:age] field/change 25)
                (form.validation/validate validation field-labels)
                validation/valid?)))))

(t/deftest field-validation
  (let [validation (form.validation/field [:age] (form.validation/non-nil))
        form       (form.validation/validate person-form validation field-labels)]
    (t/is (= ["Age is required."]
             (:field/errors (form/field-by-path form [:age]))))))

(t/deftest value-validation
  (t/testing "is long-hand for field validation"
    (let [validation (vlad/attr [:age] (form.validation/value (form.validation/non-nil)))
          form       (form.validation/validate person-form validation field-labels)]
      (t/is (= ["Age is required."]
               (:field/errors (form/field-by-path form [:age])))))))

(t/deftest complex-labels
  (let [validation (form.validation/field [:name]
                                          (vlad/attr [:person/first-name]
                                                     (form.validation/non-nil)))
        form       (form.validation/validate person-form validation field-labels)]
    (t/is (= ["First Name is required."]
             (:field/errors (form/field-by-path form [:name]))))))

(t/deftest pristine
  (let [validation (vlad/attr [:postal-code] (form.validation/not-pristine))
        form       (form.validation/validate person-form validation field-labels)]
    (t/is (= ["Postal Code must be changed."]
             (:field/errors (form/field-by-path form [:postal-code]))))
    (t/is (-> person-form
              (form/update-field-by-path [:postal-code] field/change "45678")
              (form.validation/validate validation field-labels)
              validation/valid?))))

(t/deftest urgency-level
  (t/testing "urgency of validation error can be lowered to a warning, with a corresponding message."
    (let [validation (vlad/attr [:postal-code]
                                (form.validation/warning
                                 (form.validation/not-pristine)))
          form       (form.validation/validate person-form validation field-labels)]
      (t/is (= ["Postal Code should be changed."]
               (:field/warnings (form/field-by-path form [:postal-code])))))))

(t/deftest numbers
  (let [validation (form.validation/field [:age]
                                          (vlad/chain
                                           (form.validation/pos-number)
                                           (form.validation/value-in 21 140)))
        form       (form.validation/validate person-form validation field-labels)]
    (t/is (= ["Age must be a positive number."]
             (:field/errors (form/field-by-path form [:age]))))
    (t/is (= ["Age must be between 21 and 140."]
             (-> person-form
                 (form/update-field-by-path [:age] field/change 20)
                 (form.validation/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))))

(t/deftest miscelaneous-assertions
  ;; It's not clear these should exist, since it's unlikely a user could correct
  ;; them without help from the app.
  (let [validation (form.validation/field [:age]
                                          (vlad/join
                                           (form.validation/is-uuid)
                                           (form.validation/is-inst)))
        form       (form.validation/validate person-form validation field-labels)]
    (t/is (= ["Age must be an id."
              "Age is required."]
             (:field/errors (form/field-by-path form [:age]))))))
