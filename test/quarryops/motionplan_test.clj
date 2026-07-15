(ns quarryops.motionplan-test
  "quarryops.motionplan/motion-plan-for -- the Cartesian waypoint list
  built from quarryops.robotics/mission-actions's real 3-step
  INSPECTION-ROBOT verification-mission sequence (ADR-2607995500). See
  quarryops.motionplan's own ns docstring for why this abstraction
  genuinely applies to the mission (a real multi-step process a robot
  walks through) and NOT to the physics-simulated fragment's own free-
  fall trajectory (which this ns does not, and should not, plan a route
  through)."
  (:require [clojure.test :refer [deftest is testing]]
            [quarryops.cad :as cad]
            [quarryops.motionplan :as motionplan]
            [quarryops.robotics :as robotics]))

(deftest one-waypoint-per-mission-action-same-order
  (let [plan (motionplan/motion-plan-for {:fragment-mass-kg 180.0})]
    (is (= (count robotics/mission-actions) (count plan)))
    (is (= (mapv :step robotics/mission-actions) (mapv :step plan)))
    (is (= [1 2 3] (mapv :seq plan)))
    (is (= ["bench-face-dimensional-survey" "core-sample-quality-assay" "dust-particulate-emissions-scan"]
           (mapv :station plan)))))

(deftest waypoints-are-spaced-along-the-travel-axis
  (let [plan (motionplan/motion-plan-for {:fragment-mass-kg 180.0})
        xs (mapv #(first (:waypoint %)) plan)]
    (is (= [0.0 motionplan/station-pitch-m (* 2 motionplan/station-pitch-m)] xs))
    (is (every? #(= motionplan/default-tool-orientation (:tool-orientation %)) plan))
    (is (every? #(zero? (second (:waypoint %))) plan) "y is the line centerline")))

(deftest working-height-derives-from-the-extractions-real-envelope
  (testing "z (working height) is half the extraction's own real envelope height"
    (let [extraction {:fragment-mass-kg 180.0 :fragment-height-mm 10.0}
          plan (motionplan/motion-plan-for extraction)
          z (nth (:waypoint (first plan)) 2)]
      (is (= (/ 10.0 2000.0) z))))
  (testing "an extraction with no real :fragment-height-mm still gets a real
            answer via quarryops.cad's own disclosed formula-based default
            (NOT motionplan's separate fallback), keyed off :fragment-mass-kg"
    (let [extraction {:fragment-mass-kg 180.0}
          plan (motionplan/motion-plan-for extraction)
          z (nth (:waypoint (first plan)) 2)
          expected-half-e (robotics/fragment-half-extent-m 180.0)]
      (is (< (Math/abs (- expected-half-e z)) 1e-9))))
  (testing "no extraction at all (older/hand-rolled caller) -> motionplan's own default-working-height-m"
    (let [plan (motionplan/motion-plan-for)
          z (nth (:waypoint (first plan)) 2)]
      (is (= motionplan/default-working-height-m z)))))

(deftest deterministic-same-extraction-same-plan
  (is (= (motionplan/motion-plan-for {:fragment-mass-kg 180.0 :fragment-height-mm 40.0})
         (motionplan/motion-plan-for {:fragment-mass-kg 180.0 :fragment-height-mm 40.0}))))

(deftest working-height-uses-cads-real-envelope-dims-not-a-parallel-implementation
  (testing "confirms motion-plan-for's working-height genuinely reads quarryops.
            cad/envelope-dims-mm, not a private/parallel duplication"
    (let [extraction {:fragment-mass-kg 180.0 :fragment-height-mm 88.0}
          plan (motionplan/motion-plan-for extraction)
          z (nth (:waypoint (first plan)) 2)]
      (is (= (/ (:height-mm (cad/envelope-dims-mm extraction)) 2000.0) z)))))
