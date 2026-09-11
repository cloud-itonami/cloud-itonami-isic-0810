(ns quarryops.robotics-test
  "quarryops.robotics/simulate-bench-face-settling's real physics-2d
  simulation, now bridged onto quarryops.cad's real BREP fragment
  envelope for the :fragment body's AABB (ADR-2607995500). Asserts the
  disclosed contract: (1) CAD-derived fragment geometry genuinely
  changes the simulated world (:fragment-half-extents-m, :trajectory's
  absolute positions) -- a non-cosmetic effect, and (2) an EMPIRICAL,
  swept check (not merely algebraic) of whether that same geometry
  changes :sim-impact-velocity-mps/:sim-impact-energy-j/:sim-settling-
  distance-m/:ticks -- checking THIS vertical's own actual physics
  (gravity + free-fall + settling is a different shape than autoparts'/
  fab's horizontal pull/approach), not assuming the same invariant
  autoparts/fab found carries over unchanged. See `quarryops.robotics/
  simulate-bench-face-settling`'s own docstring for the full narrative
  of what was assumed first, then verified, then corrected."
  (:require [clojure.test :refer [deftest is testing]]
            [quarryops.cad :as cad]
            [quarryops.robotics :as robotics]))

(defn- sim [extraction]
  (robotics/simulate-bench-face-settling (robotics/fragment-for extraction)))

(deftest extraction-with-no-survey-fields-is-unchanged-from-pre-adr-2607995500-behavior
  (testing "an extraction with only :fragment-mass-kg/:bench-drop-height-m
            (no real survey geometry on file) produces the SAME :fragment
            AABB half-extents this ns used as a density-derived cube
            before this ADR (quarryops.cad's defaults are defined to
            reproduce them, to within IEEE-754 double-rounding -- see
            quarryops.cad-test's own epsilon-based check of this same
            fact), and identical numeric results to an explicit-default-
            dims call"
    (let [bare (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0})
          expected-half-e (robotics/fragment-half-extent-m 180.0)
          {:keys [half-w half-h]} (:fragment-half-extents-m bare)]
      (is (< (Math/abs (- expected-half-e half-w)) 1e-9))
      (is (< (Math/abs (- expected-half-e half-h)) 1e-9))
      (is (= half-w half-h) "default envelope is still a cube"))))

(deftest cad-derived-fragment-geometry-genuinely-changes-the-fragments-aabb
  (testing "two extractions with the SAME mass/drop-height but DIFFERENT real
            :fragment-length-mm/:fragment-width-mm/:fragment-height-mm produce
            DIFFERENT :fragment-half-extents-m -- a genuine, non-cosmetic
            effect of quarryops.cad's real per-extraction geometry"
    (let [small (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0
                       :fragment-length-mm 100.0 :fragment-width-mm 100.0 :fragment-height-mm 50.0})
          large (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0
                       :fragment-length-mm 900.0 :fragment-width-mm 900.0 :fragment-height-mm 2000.0})]
      (is (not= (:fragment-half-extents-m small) (:fragment-half-extents-m large)))
      (is (= {:half-w 0.05 :half-h 0.025} (:fragment-half-extents-m small)))
      (is (= {:half-w 0.45 :half-h 1.0} (:fragment-half-extents-m large))))))

(deftest cad-derived-height-genuinely-shifts-the-trajectorys-start-position
  (testing "unlike autoparts/fab's own length/width-only physics, THIS
            vertical's fall axis is HEIGHT (see quarryops.cad's/quarryops.
            robotics/fragment-half-extents-m's docstrings) -- two
            extractions differing ONLY in :fragment-height-mm (same length/
            width/mass/drop-height) must produce a genuinely different
            fragment start-y (fragment-start-y = drop-h + half-h)"
    (let [short (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0 :fragment-height-mm 50.0})
          tall (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0 :fragment-height-mm 2000.0})
          y0 (fn [r] (second (:position (first (:trajectory r)))))]
      (is (not= (y0 short) (y0 tall)))
      (is (= 4.025 (y0 short)) "4.0 + 0.05/2 = 4.025")
      (is (= 5.0 (y0 tall)) "4.0 + 2.0/2 = 5.0")
      (is (< (y0 short) (y0 tall)))))
  (testing "changing ONLY :fragment-length-mm/:fragment-width-mm (lateral,
            NOT the fall axis) leaves the trajectory's Y start position
            untouched -- confirms length/width genuinely do NOT drive this
            vertical's fall-axis physics, the opposite role they play in
            autoparts'/fab's horizontal-travel-axis physics"
    (let [narrow (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0
                        :fragment-length-mm 50.0 :fragment-width-mm 50.0})
          wide (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0
                     :fragment-length-mm 3000.0 :fragment-width-mm 3000.0})
          y0 (fn [r] (second (:position (first (:trajectory r)))))]
      (is (= (y0 narrow) (y0 wide))
          "length/width do not enter fragment-start-y at all"))))

(deftest cad-derived-geometry-does-not-change-the-summary-readings-empirically-verified
  (testing "EMPIRICAL, not merely algebraic, check across a wide sweep of
            drop heights and fragment geometries: :sim-impact-velocity-mps/
            :sim-impact-energy-j/:ticks were found BIT-IDENTICAL regardless
            of :fragment-length-mm/:fragment-width-mm/:fragment-height-mm,
            for every (height, geometry) pair swept here -- see simulate-
            bench-face-settling's own docstring for the structural reason
            this holds robustly for this ns (a plain fixed :dt-s, not one
            specially constructed to land a collision exactly on an integer
            tick the way fab.simphysics's own approach-gap-m was), and for
            why this is reported as 'no divergence found in what was
            checked', not 'proven invariant for all possible inputs' -- an
            EARLIER DRAFT of this ns's docstring assumed a fab-style
            collision-tick knife-edge WOULD show up here by analogy; this
            test is what caught that assumption was wrong FOR THESE THREE
            fields. :sim-settling-distance-m is the ONE exception, checked
            separately below -- a genuinely different, smaller-magnitude
            finding this test ALSO caught."
    (doseq [h [0.05 0.5 1.0 2.0 3.0 4.0 5.0 7.0 9.0 12.0 15.0]
            [len wid hei] [[0.001 0.001 0.001] [50.0 50.0 50.0] [400.0 400.0 400.0]
                           [900.0 300.0 250.0] [50.0 3000.0 2500.0] [3000.0 3000.0 3000.0]]]
      (let [tiny (sim {:fragment-mass-kg 180.0 :bench-drop-height-m h})
            variant (sim {:fragment-mass-kg 180.0 :bench-drop-height-m h
                           :fragment-length-mm len :fragment-width-mm wid :fragment-height-mm hei})]
        (is (= (:sim-impact-velocity-mps tiny) (:sim-impact-velocity-mps variant))
            (str "diverged at h=" h " dims=" [len wid hei]))
        (is (= (:sim-impact-energy-j tiny) (:sim-impact-energy-j variant))
            (str "diverged at h=" h " dims=" [len wid hei]))
        (is (= (:ticks tiny) (:ticks variant))
            (str "diverged at h=" h " dims=" [len wid hei]))))))

(deftest sim-settling-distance-m-has-a-tiny-real-floating-point-divergence
  (testing "A REAL, VERIFIED, SMALLER-MAGNITUDE finding this test suite
            actually caught (an earlier draft asserted exact `=` here too,
            matching the other three summary fields -- WRONG, this test is
            what caught it): :sim-settling-distance-m (`start-y - final-y`,
            where final-y is read off tick ~3000 of a PERSISTENT resting-
            velocity oscillation -- see simulate-bench-face-settling's own
            docstring's disclosed max-ticks note) accumulates a tiny
            floating-point divergence across geometry, on the order of 1-2
            ULP (empirically bounded well under 1e-9 across an extensive
            sweep -- NOT a tick-alignment shift in the collision itself,
            since :sim-impact-velocity-mps/:sim-impact-energy-j/:ticks stay
            exactly invariant in the SAME runs, see the test above -- but
            genuine floating-point noise accumulated over ~3000 resting-
            oscillation integration steps whose exact intermediate values
            depend on half-h/half-w). Disclosed and bounded here, not
            silently rounded away or asserted as exact equality."
    (doseq [h [0.05 0.5 1.0 4.0 9.0 15.0]
            [len wid hei] [[50.0 50.0 50.0] [3000.0 3000.0 3000.0] [900.0 300.0 250.0]]]
      (let [tiny (sim {:fragment-mass-kg 180.0 :bench-drop-height-m h})
            variant (sim {:fragment-mass-kg 180.0 :bench-drop-height-m h
                           :fragment-length-mm len :fragment-width-mm wid :fragment-height-mm hei})]
        (is (< (Math/abs (- (:sim-settling-distance-m tiny) (:sim-settling-distance-m variant))) 1e-9)
            (str "unexpectedly LARGE divergence at h=" h " dims=" [len wid hei]))))))

(deftest ticks-empirically-always-hits-the-max-tick-budget-a-pre-existing-property
  (testing "NOTE, not a regression: run-until-settled's resting-contact
            dynamics under fragment-restitution 0.3 converge to a small,
            PERSISTENT, non-decaying velocity oscillation that never drops
            below settle-eps-mps for settle-run-ticks consecutive ticks, so
            :ticks is (empirically, for every input tried, incl. pre-ADR-
            2607995500 code checked directly against the original repo)
            always max-ticks+1 -- unrelated to CAD/geometry, not introduced
            or fixed by this ADR, disclosed here so :ticks assertions above
            are not mistaken for evidence of a deeper collision-timing
            invariance argument"
    (is (= 3001 (:ticks (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0}))))
    (is (= 3001 (:ticks (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 0.05}))))))

(deftest trajectory-itself-is-not-geometry-invariant-unlike-the-summary-readings
  (testing "mirrors autoparts'/fab's own disclosed trajectory-position
            sensitivity: :trajectory's actual position samples genuinely
            differ across geometry (fragment-start-y depends on half-h),
            even though the summary readings above do not"
    (let [short (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0 :fragment-height-mm 50.0})
          tall (sim {:fragment-mass-kg 180.0 :bench-drop-height-m 4.0 :fragment-height-mm 2000.0})]
      (is (not= (:trajectory short) (:trajectory tall))))))

(deftest fragment-mass-still-scales-impact-energy-independent-of-geometry
  (testing "mass legitimately scales the energy reading (0.5*mass*v^2) even
            when specimen geometry is held fixed -- the two effects (mass ->
            energy, geometry -> AABB/trajectory) are orthogonal, as
            documented; impact VELOCITY itself is mass-invariant (physics_2d
            integrates gravity independent of mass, and impulse resolution
            against a mass-0 floor is mass-invariant too -- see ns
            docstring)"
    (let [light (sim {:fragment-mass-kg 50.0 :bench-drop-height-m 4.0})
          heavy (sim {:fragment-mass-kg 500.0 :bench-drop-height-m 4.0})]
      (is (< (:sim-impact-energy-j light) (:sim-impact-energy-j heavy)))
      (is (= (:sim-impact-velocity-mps light) (:sim-impact-velocity-mps heavy))
          "impact velocity itself is mass-invariant"))))

(deftest fragment-half-extents-m-reads-quarryops-cads-real-per-extraction-dims
  (testing "fragment-half-extents-m (public -- see its own docstring for why)
            agrees with quarryops.cad/envelope-dims-mm for the same
            extraction, confirming the CAD bridge is genuinely wired in, not
            a private/parallel implementation"
    (let [extraction {:fragment-length-mm 600.0 :fragment-width-mm 300.0 :fragment-height-mm 140.0}
          {:keys [length-mm height-mm]} (cad/envelope-dims-mm extraction)]
      (is (= {:half-w (/ length-mm 2000.0) :half-h (/ height-mm 2000.0)}
             (robotics/fragment-half-extents-m extraction))))))
