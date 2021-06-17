(ns dom-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [clojure.edn :as edn]
            [f-form.dom :as form.dom]
            [f-form.field :as field]
            [f-form.field-tracker :as tracker]
            [f-form.form :as form]))

(defn update-field! [!form field-path f _e]
  (swap! !form form/update-field-by-path field-path f))

(defn mock-target-value [e] (:value (:target e)))
(defn mock-target-checked [e] (:checked (:target e)))

(t/deftest input-props
  (with-redefs [form.dom/target-value mock-target-value]
    (t/testing "creates props for an input tag"
      (let [!form         (atom (form/init [(field/init [:x] "initial" tracker/full-tracker)]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change on-focus on-blur value]}
            (form.dom/input {:on-change (partial update-field! !form)}
                            (current-field))]
        (t/is (= "initial" value))
        (on-focus {})
        (t/is (:field/active? (current-field)))
        (on-change {:target {:value "revised"}})
        (t/is (= "revised" (:field/value (current-field))))
        (on-blur {})
        (t/is (not (:field/active? (current-field))))))
    (t/testing "can parse value before applying change"
      (let [!form         (atom (form/init [(field/init [:x] nil tracker/full-tracker)]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change]}
            (f-form.dom/input {:on-change   (partial update-field! !form)
                               :parse-value edn/read-string}
                              (current-field))]
        (on-change {:target {:value "5"}})
        (t/is (= 5 (:field/value (current-field))))))
    (t/testing "maintains other props"
      (t/is (:disabled
             (f-form.dom/input {:on-change identity
                                :disabled  true}
                               (field/init [:x] nil)))))))

(t/deftest checkbox-props
  (with-redefs [form.dom/target-checked mock-target-checked]
    (t/testing "handles peculiarities of checkbox props"
      (let [!form         (atom (form/init [(field/init [:x] false)]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change checked]}
            (form.dom/checkbox {:on-change (partial update-field! !form)}
                               (current-field))]
        (t/is (= false checked))
        (on-change {:target {:checked true}})
        (t/is (= true (:field/value (current-field))))))))

(t/deftest radio-props
  (t/testing "handles peculiarities of radio props"
    (let [!form         (atom (form/init [(field/init [:x] "a")]))
          current-field #(form/field-by-path @!form [:x])
          a-props       (form.dom/radio {:on-change (partial update-field! !form)}
                                        (current-field)
                                        "a")
          b-props       (form.dom/radio {:on-change (partial update-field! !form)}
                                        (current-field)
                                        "b")]
      (t/is (= true (:checked a-props)))
      (t/is (= false (:checked b-props)))
      (let [{:keys [on-change]} b-props]
        ;; check "b"
        (on-change {})
        (t/is (= "b" (:field/value (current-field))))))))

(t/deftest select-props
  (with-redefs [form.dom/target-value mock-target-value]
    (t/testing "automatically converts complex objects to and from simple strings"
      (let [options       [{:option/id   "a"
                            :option/attr :a}
                           {:option/id   "b"
                            :option/attr :b}
                           {:option/id   "c"
                            :option/attr :c}]
            !form         (atom (form/init [(field/init [:x] (first options))]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change value]}
            (f-form.dom/select {:on-change (partial update-field! !form)}
                               (current-field)
                               {:options      options
                                :option-value :option/id})]
        (t/is (= "a" value))
        (on-change {:target {:value "b"}})
        (t/is (= {:option/id   "b"
                  :option/attr :b}
                 (:field/value (current-field))))))
    (t/testing "does not require an initial value"
      (let [options [{:option/id   "a"
                      :option/attr :a}
                     {:option/id   "b"
                      :option/attr :b}
                     {:option/id   "c"
                      :option/attr :c}]
            form    (form/init [(field/init [:x])])

            {:keys [value]}
            (f-form.dom/select {:on-change (fn [_])}
                               (form/field-by-path form [:x])
                               {:options      options
                                :option-value :option/id})]
        (t/is (= "" value))))))

(t/deftest plugin-props
  (with-redefs [form.dom/target-value mock-target-value]
    (t/testing "assumes input is parsed before on-change is called"
      (let [!form         (atom (form/init [(field/init [:x] nil)]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change]}
            (f-form.dom/plugin {:on-change (partial update-field! !form)}
                               (current-field))]
        (on-change {:entity/attr "value"})
        (t/is (= {:entity/attr "value"}
                 (:field/value (current-field))))))))
