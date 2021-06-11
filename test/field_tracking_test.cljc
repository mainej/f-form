(ns field-tracking-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [f-form.field :as field]
            [f-form.field-tracker :as tracker]
            [f-form.form :as form]))

(t/deftest navigating-between-fields
  (t/testing "tracks the movement"
    (let [form (form/init [(field/init [:x] nil tracker/full-tracker)
                           (field/init [:y] nil tracker/full-tracker)])]
      ;; Before entering form
      (t/is (not (:field/visited? (form/field-by-path form [:x]))))
      (t/is (not (:field/active? (form/field-by-path form [:x]))))
      (t/is (not (:field/touched? (form/field-by-path form [:x]))))
      (t/is (not (:field/visited? (form/field-by-path form [:y]))))
      (t/is (not (:field/active? (form/field-by-path form [:y]))))
      (t/is (not (:field/touched? (form/field-by-path form [:y]))))
      ;; Onto :x
      (let [form (form/update-field-by-path form [:x] field/gain-focus)]
        (t/is (:field/visited? (form/field-by-path form [:x])))
        (t/is (:field/active? (form/field-by-path form [:x])))
        (t/is (not (:field/touched? (form/field-by-path form [:x]))))
        ;; From :x to :y
        (let [form (-> form
                       (form/update-field-by-path [:x] field/lose-focus)
                       (form/update-field-by-path [:y] field/gain-focus))]
          (t/is (:field/visited? (form/field-by-path form [:x])))
          (t/is (not (:field/active? (form/field-by-path form [:x]))))
          (t/is (:field/touched? (form/field-by-path form [:x])))
          (t/is (:field/visited? (form/field-by-path form [:y])))
          (t/is (:field/active? (form/field-by-path form [:y])))
          (t/is (not (:field/touched? (form/field-by-path form [:y]))))
          ;; Off :y
          (let [form (form/update-field-by-path form [:y] field/lose-focus)]
            (t/is (:field/visited? (form/field-by-path form [:y])))
            (t/is (not (:field/active? (form/field-by-path form [:y]))))
            (t/is (:field/touched? (form/field-by-path form [:y])))))))))

(t/deftest filling-fields
  (t/testing "tracks the changes"
    (let [form (form/init [(field/init [:x] "initial" tracker/full-tracker)])]
      ;; Before change
      (t/is (:field/pristine? (form/field-by-path form [:x])))
      (t/is (not (:field/modified? (form/field-by-path form [:x]))))
      ;; After change
      (let [form (form/update-field-by-path form [:x] #(field/change % "revised"))]
        (t/is (not (:field/pristine? (form/field-by-path form [:x]))))
        (t/is (:field/modified? (form/field-by-path form [:x])))
        ;; After revert
        (let [form (form/update-field-by-path form [:x] #(field/change % "initial"))]
          (t/is (:field/pristine? (form/field-by-path form [:x])))
          (t/is (:field/modified? (form/field-by-path form [:x]))))))))

(t/deftest with-partial-tracker
  (t/testing "tracks some movement"
    (let [form (form/init [(field/init [:x] nil tracker/default-tracker)])]
      ;; Before entering form
      (t/is (nil? (:field/visited? (form/field-by-path form [:x]))))
      (t/is (nil? (:field/active? (form/field-by-path form [:x]))))
      (t/is (not (:field/touched? (form/field-by-path form [:x]))))
      ;; Onto :x
      (let [form (form/update-field-by-path form [:x] field/gain-focus)]
        (t/is (nil? (:field/visited? (form/field-by-path form [:x]))))
        (t/is (nil? (:field/active? (form/field-by-path form [:x]))))
        (t/is (not (:field/touched? (form/field-by-path form [:x]))))
        ;; Off :x
        (let [form (form/update-field-by-path form [:x] field/lose-focus)]
          (t/is (nil? (:field/visited? (form/field-by-path form [:x]))))
          (t/is (nil? (:field/active? (form/field-by-path form [:x]))))
          (t/is (:field/touched? (form/field-by-path form [:x])))))))
  (t/testing "tracks some changes"
    (let [form (form/init [(field/init [:x] "initial" (tracker/tracker #{:field/modified?}))])]
      ;; Before change
      (t/is (nil? (:field/pristine? (form/field-by-path form [:x]))))
      (t/is (not (:field/modified? (form/field-by-path form [:x]))))
      ;; After change
      (let [form (form/update-field-by-path form [:x] #(field/change % "revised"))]
        (t/is (nil? (:field/pristine? (form/field-by-path form [:x]))))
        (t/is (:field/modified? (form/field-by-path form [:x])))
        ;; After revert
        (let [form (form/update-field-by-path form [:x] #(field/change % "initial"))]
          (t/is (nil? (:field/pristine? (form/field-by-path form [:x]))))
          (t/is (:field/modified? (form/field-by-path form [:x]))))))))

(t/deftest after-snapshot
  (t/testing "captures new value, but doesn't change movement history"
    (let [form (-> (form/init [(field/init [:x] "initial" tracker/full-tracker)])
                   (form/update-field-by-path [:x] field/gain-focus)
                   (form/update-field-by-path [:x] #(field/change % "revised"))
                   (form/update-field-by-path [:x] field/lose-focus)
                   (form/update-field-by-path [:x] field/snapshot))]
      (t/is (:field/visited? (form/field-by-path form [:x])))
      (t/is (not (:field/active? (form/field-by-path form [:x]))))
      (t/is (:field/touched? (form/field-by-path form [:x])))
      (t/is (:field/pristine? (form/field-by-path form [:x])))
      (t/is (not (:field/modified? (form/field-by-path form [:x])))))))

(t/deftest after-reset
  (t/testing "resets movement and change history"
    (let [form (-> (form/init [(field/init [:x] "initial" tracker/full-tracker)])
                   (form/update-field-by-path [:x] field/gain-focus)
                   (form/update-field-by-path [:x] #(field/change % "revised"))
                   (form/update-field-by-path [:x] field/lose-focus)
                   (form/update-field-by-path [:x] field/reset))]
      (t/is (not (:field/visited? (form/field-by-path form [:x]))))
      (t/is (not (:field/active? (form/field-by-path form [:x]))))
      (t/is (not (:field/touched? (form/field-by-path form [:x]))))
      (t/is (:field/pristine? (form/field-by-path form [:x])))
      (t/is (not (:field/modified? (form/field-by-path form [:x])))))))
