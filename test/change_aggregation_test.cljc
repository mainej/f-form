(ns change-aggregation-test
  (:require  #?(:clj [clojure.test :as t]
                :cljs [cljs.test :as t :include-macros true])
             [f-form.form :as form]
             [f-form.field :as field]
             [f-form.field-tracker :as tracker]))

(def pristine-tracker (tracker/tracker #{:field/pristine?}))

(t/deftest collects-changed-fields
  (t/is (= {:x {:a "revised"
                :b "revised"}
            :y {:a "revised"}}
         (-> (form/init [(field/init [:x :a] "initial" pristine-tracker)
                         (field/init [:x :b] "initial" pristine-tracker)
                         (field/init [:y :a] "initial" pristine-tracker)
                         (field/init [:y :b] "initial" pristine-tracker)])
             (form/update-field-by-path [:x :a] #(field/change % "revised"))
             (form/update-field-by-path [:x :b] #(field/change % "revised"))
             (form/update-field-by-path [:y :a] #(field/change % "revised"))
             form/changes))))
