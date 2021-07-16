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

(defrecord MockEvent [target])
(defrecord MockInputElement [value checked])

(defn mock-event
  ([] (mock-event nil nil))
  ([v b] (map->MockEvent {:target (map->MockInputElement {:value v :checked b})})))

(defn mock-value-event   [v] (mock-event v nil))
(defn mock-checked-event [b] (mock-event nil b))

(t/deftest input-props
  (t/testing "creates props for an input tag"
    (let [!form         (atom (form/init [(field/init [:x] "initial" tracker/full-tracker)]))
          current-field #(form/field-by-path @!form [:x])

          {:keys [on-change on-focus on-blur value]}
          (form.dom/input {:on-change (partial update-field! !form)}
                          (current-field))]
      (t/is (= "initial" value))
      (on-focus (mock-event))
      (t/is (:field/active? (current-field)))
      (on-change (mock-value-event "revised"))
      (t/is (= "revised" (:field/value (current-field))))
      (on-blur (mock-event))
      (t/is (not (:field/active? (current-field))))))
  (t/testing "can parse value before applying change"
    (let [!form         (atom (form/init [(field/init [:x] nil tracker/full-tracker)]))
          current-field #(form/field-by-path @!form [:x])

          {:keys [on-change]}
          (f-form.dom/input {:on-change   (partial update-field! !form)
                             :parse-value edn/read-string}
                            (current-field))]
      (on-change (mock-value-event "5"))
      (t/is (= 5 (:field/value (current-field))))))
  (t/testing "casts empty string to nil"
    (let [!form         (atom (form/init [(field/init [:x] "initial")]))
          current-field #(form/field-by-path @!form [:x])

          {:keys [on-change]}
          (f-form.dom/input {:on-change (partial update-field! !form)}
                            (current-field))]
      (on-change (mock-value-event ""))
      (t/is (nil? (:field/value (current-field))))))
  (t/testing "maintains other props"
    (t/is (:disabled
           (f-form.dom/input {:on-change identity
                              :disabled  true}
                             (field/init [:x] nil))))))

(t/deftest checkbox-props
  (t/testing "handles peculiarities of checkbox props"
    (let [!form         (atom (form/init [(field/init [:x] false)]))
          current-field #(form/field-by-path @!form [:x])

          {:keys [on-change checked]}
          (form.dom/checkbox {:on-change (partial update-field! !form)}
                             (current-field))]
      (t/is (= false checked))
      (on-change (mock-checked-event true))
      (t/is (= true (:field/value (current-field)))))))

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
        (on-change (mock-event))
        (t/is (= "b" (:field/value (current-field))))))))

(t/deftest select-props
  (let [options [{:option/id   "a"
                  :option/attr :a}
                 {:option/id   "b"
                  :option/attr :b}
                 {:option/id   "c"
                  :option/attr :c}]]
    (t/testing "automatically converts complex objects to and from simple strings"
      (let [!form         (atom (form/init [(field/init [:x] (first options))]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [on-change value]}
            (f-form.dom/select {:on-change (partial update-field! !form)}
                               (current-field)
                               {:options      options
                                :option-value :option/id})]
        (t/is (= "a" value))
        (on-change (mock-value-event "b"))
        (t/is (= {:option/id   "b"
                  :option/attr :b}
                 (:field/value (current-field))))))
    (t/testing "does not require an initial value"
      (let [!form         (atom (form/init [(field/init [:x] nil)]))
            current-field #(form/field-by-path @!form [:x])

            {:keys [value]}
            (f-form.dom/select {:on-change (partial update-field! !form)}
                               (current-field)
                               {:options      options
                                :option-value :option/id})]
        (t/is (= "" value))))))

(t/deftest plugin-props
  (t/testing "assumes input is parsed before on-change is called"
    (let [!form         (atom (form/init [(field/init [:x] nil)]))
          current-field #(form/field-by-path @!form [:x])

          {:keys [on-change]}
          (f-form.dom/plugin {:on-change (partial update-field! !form)}
                             (current-field))]
      (on-change {:entity/attr "value"})
      (t/is (= {:entity/attr "value"}
               (:field/value (current-field)))))))
