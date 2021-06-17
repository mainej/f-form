(ns form-management-test
  "This test namespace may seem small, but it's not trying to be a comprehensive
  test of form functionality. Instead, it focuses on a few things not covered by
  other tests. See the change tracking, dom and validation tests for more
  thorough testing of the common features."
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [f-form.field :as field]
            [f-form.form :as form]))

(t/deftest removing-fields
  (t/testing "leaves form in a pristine state"
    (t/is (= (form/init [(field/init [:another-val])])
             (-> (form/init [(field/init [:val :sub-val])
                             (field/init [:val :another-sub-val])
                             (field/init [:another-val])])
                 (form/remove-field-by-path [:val :sub-val])
                 (form/remove-field-by-path [:val :another-sub-val])
                 (form/remove-field-by-path [:never-a-val :nested]))))))

(t/deftest updating-field
  (t/testing "ignores undefined fields"
    (let [form (form/init [(field/init [:val])])]
      (t/is (= form
               (form/update-field-by-path form [:does-not-exist] (fn [f] (field/change f "new value"))))))))

(t/deftest fetching-value-by-path
  (t/is (= "value"
           (form/value-by-path (form/init [(field/init [:val] "value")])
                               [:val]))))

(t/deftest fetching-all-values
  (t/is (= {:val     "value"
            :nested  {:val "nested"}
            :complex {:id  "c"
                      :val "complex"}}
           (form/values (form/init [(field/init [:val] "value")
                                    (field/init [:nested :val] "nested")
                                    (field/init [:complex] {:id  "c"
                                                            :val "complex"})])))))

(t/deftest submitting
  (t/is (not (form/submitting? (form/init))))
  (t/is (form/submitting? (form/submitting (form/init))))
  (t/is (not (form/submitting? (form/submitted (form/submitting (form/init)))))))
