(ns vlad-validation-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [f-form.field :as field]
            [f-form.field-tracker :as tracker]
            [f-form.form :as form]
            [f-form.validation :as validation]
            [f-form.validation.vlad :as form.vlad]
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
                    (form.vlad/field [:name]
                                     (vlad/join
                                      (vlad/attr [:person/first-name]
                                                 (form.vlad/non-nil))
                                      (vlad/attr [:person/last-name]
                                                 (form.vlad/non-nil))))
                    (form.vlad/field [:age]
                                     (vlad/chain
                                      (form.vlad/pos-number)
                                      (form.vlad/value-in 21 140))))
        form       (form.vlad/validate person-form validation field-labels)]
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
                (form.vlad/validate validation field-labels)
                validation/valid?)))))

(t/deftest field-validation
  (let [validation (form.vlad/field [:age] (form.vlad/non-nil))]
    (t/is (= ["Age is required."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))))

(t/deftest value-validation
  (t/testing "is long-hand for field validation"
    (let [validation (vlad/attr [:age] (form.vlad/value (form.vlad/non-nil)))]
      (t/is (= ["Age is required."]
               (-> person-form
                   (form.vlad/validate validation field-labels)
                   (form/field-by-path [:age])
                   :field/errors))))))

(t/deftest complex-labels
  (let [validation (form.vlad/field [:name]
                                    (vlad/attr [:person/first-name]
                                               (form.vlad/non-nil)))]
    (t/is (= ["First Name is required."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:name])
                 :field/errors)))))

(t/deftest pristine
  (let [validation (vlad/attr [:postal-code] (form.vlad/not-pristine))]
    (t/is (= ["Postal Code must be changed."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:postal-code])
                 :field/errors)))
    (t/is (-> person-form
              (form/update-field-by-path [:postal-code] field/change "45678")
              (form.vlad/validate validation field-labels)
              validation/valid?))))

(t/deftest urgency-level
  (t/testing "urgency of validation error can be lowered to a warning, with a corresponding message."
    (let [validation (vlad/attr [:postal-code]
                                (form.vlad/warning
                                 (form.vlad/not-pristine)))
          form       (form.vlad/validate person-form validation field-labels)]
      (t/is (= ["Postal Code should be changed."]
               (-> form
                   (form/field-by-path [:postal-code])
                   :field/warnings)))
      (t/is (validation/valid? form)))))

(t/deftest numbers
  (let [validation (form.vlad/field [:age]
                                    (vlad/chain
                                     (form.vlad/pos-number)
                                     (form.vlad/value-in 21 140)))]
    (t/is (= ["Age must be a positive number."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))
    (t/is (= ["Age must be between 21 and 140."]
             (-> person-form
                 (form/update-field-by-path [:age] field/change 20)
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))))

(t/deftest miscelaneous-assertions
  ;; It's not clear these should exist, since it's unlikely a user could correct
  ;; them without help from the app.
  (let [validation (form.vlad/field [:age]
                                    (vlad/join
                                     (form.vlad/is-uuid)
                                     (form.vlad/is-inst)))]
    (t/is (= ["Age must be an id."
              "Age is required."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))))
