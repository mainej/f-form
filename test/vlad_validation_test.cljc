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
                 :field/errors))))
  (t/testing "ignores empty field validation"
    (let [validation (form.vlad/field [:age])]
      (t/is (-> person-form
                (form.vlad/validate validation field-labels)
                validation/valid?)))))

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
                 :field/errors)))
    (t/is (= ["Age must be between 21 and 140."]
             (-> person-form
                 (form/update-field-by-path [:age] field/change 141)
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:age])
                 :field/errors)))
    (t/is (-> person-form
              (form/update-field-by-path [:age] field/change 140)
              (form.vlad/validate validation field-labels)
              validation/valid?))
    (t/is (-> person-form
              (form/update-field-by-path [:age] field/change 21)
              (form.vlad/validate validation field-labels)
              validation/valid?))))

(t/deftest custom-error-message
  (let [validation (vlad/attr [:postal-code] (form.vlad/not-pristine {:message "Zip hasn't been changed."}))]
    (t/is (= ["Zip hasn't been changed."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:postal-code])
                 :field/errors)))))

(t/deftest custom-field-name
  (let [validation (vlad/attr [:postal-code] (form.vlad/not-pristine {:name "Zip"}))]
    (t/is (= ["Zip must be changed."]
             (-> person-form
                 (form.vlad/validate validation field-labels)
                 (form/field-by-path [:postal-code])
                 :field/errors)))))

(t/deftest miscelaneous-assertions
  (t/testing "f-form defined errors"
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
  (t/testing "vlad defined errors"
    (letfn [(validated-field [value validation]
              (let [path [:path]]
                (-> (form/init [(field/init path value)])
                    (form.vlad/validate (form.vlad/field path validation) {path "Field"})
                    (form/field-by-path path))))]
      (t/is (= ["Field is required."]
               (:field/errors (validated-field nil (vlad/present)))))
      (t/is (= ["Field is expected."]
               (:field/warnings (validated-field nil (form.vlad/warning (vlad/present))))))
      (t/is (= ["Field must be over 6 characters long."]
               (:field/errors (validated-field "123456" (vlad/length-over 6)))))
      (t/is (= ["Field must be under 6 characters long."]
               (:field/errors (validated-field "123456" (vlad/length-under 6)))))
      (t/is (= ["Field must be one of a, b, c."]
               (:field/errors (validated-field "z" (vlad/one-of #{"a" "b" "c"})))))
      (t/is (= ["Field must not be one of a, b, c."]
               (:field/errors (validated-field "a" (vlad/not-of #{"a" "b" "c"})))))
      (t/is (= ["Field must be \"a\"."]
               (:field/errors (validated-field "z" (vlad/equals-value "a")))))
      (t/is (= ["Field must match the pattern a."]
               (:field/errors (validated-field "z" (vlad/matches #"a")))))
      ;; This is not the only way to do confirmations. See
      ;; examples/reagent/src/app/hello_form.cljs for a different approach.
      (t/is (= ["Password must be the same as Confirmation."]
               (-> (form/init [(field/init [:password] {:main         "a"
                                                        :confirmation "b"})])
                   (form.vlad/validate
                    (form.vlad/field [:password] (vlad/equals-field
                                                  [:main]
                                                  [:confirmation]
                                                  {:first-name  "Password"
                                                   :second-name "Confirmation"}))
                    {})
                   (form/field-by-path [:password])
                   :field/errors))))))
