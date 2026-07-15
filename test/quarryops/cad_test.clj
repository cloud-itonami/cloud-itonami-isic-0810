(ns quarryops.cad-test
  "quarryops.cad's real BREP loose-block/fragment-envelope bridge
  (ADR-2607995500) -- envelope-dims-mm's real-survey-vs-formula-default
  fallback discipline (NOTE: unlike autoparts.cad/fab.cad, the default
  here is a FORMULA of :fragment-mass-kg, not a fixed constant -- see
  quarryops.cad's own docstring for why), and envelope-solid/
  envelope-mesh's genuine tessellation output."
  (:require [clojure.test :refer [deftest is testing]]
            [quarryops.cad :as cad]
            [quarryops.robotics :as robotics]))

(deftest default-fragment-half-extent-m-reproduces-robotics-prior-formula
  (testing "quarryops.cad's redefinition of the cube-root-of-mass/density
            formula is IDENTICAL (not merely epsilon-close) to quarryops.
            robotics/fragment-half-extent-m's own formula for a range of
            masses -- both are the exact same expression, so this must
            hold exactly, not just approximately"
    (doseq [mass [1.0 50.0 150.0 340.0 1000.0]]
      (is (= (robotics/fragment-half-extent-m mass)
             (cad/default-fragment-half-extent-m mass))
          (str "diverged at mass=" mass)))))

(deftest envelope-dims-mm-falls-back-to-the-disclosed-formula-when-absent
  (testing "an extraction with no :fragment-*-mm fields gets the SAME
            cube edge quarryops.robotics/fragment-half-extent-m always
            derived from :fragment-mass-kg alone, to within IEEE-754
            double-rounding of the mm round-trip (mirrors fab.cad-test's
            own epsilon-based check of this same kind of fact)"
    (let [extraction {:id "extraction-x" :fragment-mass-kg 180.0}
          dims (cad/envelope-dims-mm extraction)
          expected-half-e (robotics/fragment-half-extent-m 180.0)]
      (is (= (:length-mm dims) (:width-mm dims) (:height-mm dims))
          "default envelope is a cube, exactly like the pre-ADR behavior")
      (is (< (Math/abs (- expected-half-e (/ (:length-mm dims) 2000.0))) 1e-9))))
  (testing "nil extraction also falls back cleanly, at default-fragment-mass-kg"
    (let [dims (cad/envelope-dims-mm nil)
          expected-half-e (robotics/fragment-half-extent-m cad/default-fragment-mass-kg)]
      (is (= (:length-mm dims) (:width-mm dims) (:height-mm dims)))
      (is (< (Math/abs (- expected-half-e (/ (:length-mm dims) 2000.0))) 1e-9)))))

(deftest envelope-dims-mm-uses-an-extractions-own-real-survey-measurement-when-present
  (testing "an explicit :fragment-*-mm triple overrides the formula default"
    (is (= {:length-mm 900.0 :width-mm 600.0 :height-mm 450.0}
           (cad/envelope-dims-mm {:fragment-mass-kg 180.0
                                   :fragment-length-mm 900.0
                                   :fragment-width-mm 600.0
                                   :fragment-height-mm 450.0}))))
  (testing "a partial triple only overrides the fields actually given -- the
            other two still fall back to the SAME mass-derived formula"
    (let [dims (cad/envelope-dims-mm {:fragment-mass-kg 180.0 :fragment-length-mm 900.0})
          expected-half-e (robotics/fragment-half-extent-m 180.0)]
      (is (= 900.0 (:length-mm dims)))
      (is (< (Math/abs (- expected-half-e (/ (:width-mm dims) 2000.0))) 1e-9))
      (is (< (Math/abs (- expected-half-e (/ (:height-mm dims) 2000.0))) 1e-9)))))

(deftest envelope-dims-mm-vary-per-extraction
  (testing "two extractions with different real survey measurements get
            genuinely different envelopes -- this is not a fixed constant
            dressed up as per-extraction data"
    (is (not= (cad/envelope-dims-mm {:fragment-length-mm 400.0 :fragment-width-mm 300.0 :fragment-height-mm 250.0})
              (cad/envelope-dims-mm {:fragment-length-mm 900.0 :fragment-width-mm 700.0 :fragment-height-mm 600.0}))))
  (testing "two extractions with only DIFFERENT masses (no survey fields) also
            get genuinely different default envelopes -- the default is a
            formula of mass, not a single fixed constant"
    (is (not= (cad/envelope-dims-mm {:fragment-mass-kg 50.0})
              (cad/envelope-dims-mm {:fragment-mass-kg 500.0})))))

(deftest envelope-solid-produces-real-tessellatable-geometry
  (let [{:keys [dims] :as solid} (cad/envelope-solid {:fragment-length-mm 900.0
                                                        :fragment-width-mm 600.0
                                                        :fragment-height-mm 450.0})]
    (is (= {:length-mm 900.0 :width-mm 600.0 :height-mm 450.0} dims))
    (is (seq (:vertices solid)))
    (is (seq (:edges solid)))
    (testing "the tessellated footprint's X/Y extent matches the requested dims (mm)"
      (let [{:keys [positions]} (cad/envelope-mesh solid)
            extent (fn [axis] (- (apply max (map #(nth % axis) positions))
                                  (apply min (map #(nth % axis) positions))))]
        (is (< (Math/abs (- (extent 0) 900.0)) 1e-6))
        (is (< (Math/abs (- (extent 1) 600.0)) 1e-6))))))

(deftest envelope-mesh-is-well-formed
  (let [solid (cad/envelope-solid {:fragment-mass-kg 180.0})
        {:keys [positions indices]} (cad/envelope-mesh solid)]
    (is (pos? (count positions)))
    (is (pos? (count indices)))
    (is (zero? (mod (count indices) 3)) "indices are complete triangles")
    (is (every? #(<= 0 % (dec (count positions))) indices)
        "every index references a valid vertex")
    (is (every? #(= 3 (count %)) positions) "positions are [x y z]")))
