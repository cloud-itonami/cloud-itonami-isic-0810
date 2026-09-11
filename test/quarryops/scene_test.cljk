(ns quarryops.scene-test
  "quarryops.scene's bridge from quarryops.cad's tessellated envelope +
  quarryops.robotics/simulate-bench-face-settling's trajectory into
  kami.webgpu.mesh's real input shape, asserted for well-formedness --
  no browser/WebGPU device is available in this JVM/.cljc actor repo
  (see quarryops.scene's docstring). Direct port of autoparts.scene-
  test's/fab.scene-test's own assertions (ADR-2607160000/ADR-2607992500),
  adapted to a plain extraction map instead of a part-lot/lot."
  (:require [clojure.test :refer [deftest is testing]]
            [quarryops.robotics :as robotics]
            [quarryops.scene :as scene]))

(def ^:private sample-extraction
  {:id "extraction-scene-test" :fragment-mass-kg 180.0 :bench-drop-height-m 4.0
   :fragment-length-mm 350.0 :fragment-width-mm 300.0 :fragment-height-mm 250.0})

(deftest mesh-data-is-well-formed
  (testing "positions/normals/indices satisfy kami.webgpu.mesh/upload-mesh!'s
            real contract: same-length positions/normals, index count a
            multiple of 3, every index within the vertex range"
    (let [{:keys [positions normals indices vertex-count index-count]} (scene/scene-for sample-extraction)]
      (is (pos? vertex-count))
      (is (pos? index-count))
      (is (= (count positions) vertex-count))
      (is (= (count normals) vertex-count)
          "upload-mesh! requires one normal per vertex, not optional like uvs/skin/morph")
      (is (= (count indices) index-count))
      (is (zero? (mod index-count 3)))
      (is (every? #(<= 0 % (dec vertex-count)) indices)
          "every index must reference a valid vertex")
      (is (every? #(= 3 (count %)) positions) "positions are [x y z]")
      (is (every? #(= 3 (count %)) normals) "normals are [x y z]")
      (is (every? (fn [n] (< (Math/abs (- 1.0 (Math/sqrt (reduce + (map * n n))))) 1e-6)) normals)
          "every normal must actually be unit-length"))))

(deftest one-frame-per-simulated-tick
  (testing "one :transform per quarryops.robotics/simulate-bench-face-settling
            trajectory tick"
    (let [sim (robotics/simulate-bench-face-settling (robotics/fragment-for sample-extraction))
          sc (scene/scene-for sample-extraction)]
      (is (= (:ticks sim) (count (:frames sc))))
      (is (every? #(= 3 (count (get-in % [:transform :translation]))) (:frames sc)))
      (is (every? #(= [0.0 0.0 0.0] (get-in % [:transform :rotation])) (:frames sc))
          "physics-2d has no orientation state -- every frame's rotation is identity, honestly")
      (is (every? #(= [1.0 1.0 1.0] (get-in % [:transform :scale])) (:frames sc)))
      ;; translations move: the scene isn't rendering a frozen frame (the
      ;; fragment free-falls then settles, unlike a static point-test).
      (is (not= (get-in (first (:frames sc)) [:transform :translation])
                (get-in (last (:frames sc)) [:transform :translation]))))))

(deftest mesh-is-unit-converted-to-meters-and-already-centered-in-xy
  (testing "the mesh's XY footprint extent (now in METERS, matching
            quarryops.robotics's trajectory units) still matches the real
            envelope-dims-mm length/width (converted mm->m); X/Y are
            naturally centered on the local origin already (quarryops.cad's
            +/-0.5-unit-square sketch convention -- see quarryops.scene's
            docstring)"
    (let [{:keys [positions dims]} (scene/scene-for sample-extraction)
          extent (fn [axis] (- (apply max (map #(nth % axis) positions))
                                (apply min (map #(nth % axis) positions))))]
      (is (< (Math/abs (- (extent 0) (/ (:length-mm dims) 1000.0))) 1e-6))
      (is (< (Math/abs (- (extent 1) (/ (:width-mm dims) 1000.0))) 1e-6))
      ;; centered: min/max along X (and Y) are symmetric around 0.
      (is (< (Math/abs (+ (apply min (map #(nth % 0) positions))
                          (apply max (map #(nth % 0) positions))))
             1e-6)))))

(deftest scene-for-falls-back-to-quarryops-robotics-own-defaults-when-fields-absent
  (testing "an extraction with no :fragment-mass-kg/:bench-drop-height-m at
            all still works through scene-for -- quarryops.robotics/
            fragment-for supplies the SAME disclosed defaults quarryops.
            robotics always used, and quarryops.cad falls back to its own
            formula-based default for the envelope dims"
    (let [sc (scene/scene-for {:id "extraction-bare"})]
      (is (pos? (:vertex-count sc)))
      (is (pos? (:index-count sc))))))
