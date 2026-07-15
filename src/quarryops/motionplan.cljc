(ns quarryops.motionplan
  "Extends `quarryops.robotics/mission-actions` -- the 3-step bench-face-
  dimensional-survey / core-sample-quality-assay / dust-particulate-
  emissions-scan robot verification mission every extraction already
  walks through (`quarryops.robotics/simulate-quarry-face-verification`)
  -- into an actual ordered list of Cartesian waypoints, one per mission
  action, walking the SAME action order the real mission already commits
  to the audit ledger (ADR-2607995500 -- direct port of `autoparts.
  motionplan`'s/`fab.motionplan`'s reference pattern, ADR-2607160000/
  ADR-2607992500, to this vertical's own case, itself a port of
  `kami-engine-vehicle-designer`'s `vdesign.motionplan`, ADR-2607151600).

  APPLICABILITY, CHECKED FOR THIS VERTICAL, NOT ASSUMED (ADR-2607995500
  explicitly calls for this): this vertical's PHYSICS simulation
  (`quarryops.robotics/simulate-bench-face-settling`) models a loose
  block FREE-FALLING under gravity -- a single body's own uncontrolled
  fall/bounce/settle trajectory, not a multi-station route a robot
  drives through. A waypoint list would make NO physical sense for
  THAT trajectory, and this ns does NOT attempt to plan one for it.
  What this ns actually plans a route through is a SEPARATE, genuinely
  multi-step process: the INSPECTION ROBOT'S OWN verification mission
  (`quarryops.robotics/mission-actions`) -- three distinct sensing/
  assay/scan actions the SAME robot performs in sequence at/near the
  quarry face BEFORE any extraction proposal may commit. This is the
  exact same shape `autoparts.motionplan`'s CMM-scan/torque-check/
  ultrasonic-scan inspection-cell mission and `fab.motionplan`'s wafer-
  probe/optical-inspection/wire-bond-pull-test cleanroom-cell mission
  already plan a waypoint list for -- a real, pre-existing, ordered,
  multi-action process this actor's own governor already gates
  (`quarryops.phase`'s `:robotics/simulate-quarry-face-verification`
  op), not the physics-simulated body's own free-fall path. So the
  SAME waypoint-list abstraction DOES genuinely apply here, for the
  SAME reason it applies to autoparts/fab -- verified by checking this
  vertical's own actual data model (`mission-actions` IS a real 3-step
  ordered sequence here too), not copied on the assumption every
  vertical's shape matches.

  Honest scope, HONEST DESIGN CHOICE disclosed (mirrors `quarryops.cad`
  and `quarryops.robotics`'s own disclosed choices, and `autoparts.
  motionplan`'s/`fab.motionplan`'s before them): `vdesign.motionplan`
  extends `vdesign.process/plan`'s real multi-station BOM + 4D
  assembly-order sequence (the giemon-factory `construction.order.json
  :seq` pattern) -- but THIS repo has no multi-station BOM/assembly-
  order system at all, and ADR-2607160000 (which this ADR follows)
  explicitly directs NOT inventing one just to mirror automotive's
  shape. Instead this ns reuses `quarryops.robotics/mission-actions`'s
  existing, REAL 3-step list AS the station sequence -- the same 3
  actions `simulate-quarry-face-verification` already runs and
  records, walked in the same order, never a new invented process
  model.

  This is a WAYPOINT LIST -- a plausible, honestly simplified layout
  (mission actions placed at a fixed pitch along a straight line,
  working height derived from the extraction's own real fragment-
  envelope dims via `quarryops.cad`) -- NOT an inverse-kinematics
  solver, NOT a trajectory optimizer, and it does not drive any real
  robot controller. `:tool-orientation` is a fixed 'straight down'
  approach vector, not a solved end-effector pose.

  `:station` is each action's own `:step` keyword name (as a string):
  this actor's data model has no separate station-naming concept the
  way `vdesign.process/plan`'s multi-station BOM does (every action
  runs at/near the SAME `:robot/quarry-face-cell-1`, see `quarryops.
  robotics/simulate-quarry-face-verification`), so the mission step
  honestly doubles as its own station identity rather than inventing
  station names this actor's data has never had. Spacing the 3 actions
  along a line by `station-pitch-m` is the SAME simplifying convention
  `vdesign.motionplan`/`autoparts.motionplan`/`fab.motionplan` use for
  their own multi-/single-station layouts, reused here even though
  this actor's own actions likely run at or near one physical survey
  station -- disclosed, not hidden."
  (:require [quarryops.cad :as cad]
            [quarryops.robotics :as robotics]))

(def ^:const station-pitch-m
  "Nominal spacing between adjacent mission-action waypoints (m) -- a
  plausible, round figure, honestly NOT derived from any real quarry-
  face survey station's actual layout (mirrors `autoparts.motionplan/
  station-pitch-m`/`fab.motionplan/station-pitch-m`, itself scaled down
  from automotive's 5.0 m assembly-line figure to a plausible single-
  cell scale; reused verbatim here at the same 1.5 m plausible single-
  cell scale)."
  1.5)

(def ^:const default-tool-orientation
  "Fixed straight-down tool-approach vector -- NOT a solved end-
  effector orientation (this namespace is not an IK solver; mirrors
  `autoparts.motionplan/default-tool-orientation`/`fab.motionplan/
  default-tool-orientation`)."
  [0.0 0.0 -1.0])

(def ^:const default-working-height-m
  "Fallback working height (m) when `motion-plan-for` is called with no
  extraction at all (mirrors `autoparts.motionplan/default-working-
  height-m`/`fab.motionplan/default-working-height-m`)."
  0.75)

(defn- working-height-m
  "Half the extraction's own real tessellated fragment-envelope height
  (`quarryops.cad/envelope-dims-mm`) -- a plausible fixed working
  height for every action, not a per-action solved height. Falls back
  to `default-working-height-m` only when `extraction` itself is nil
  (an older/hand-rolled caller with nothing to read at all); an
  extraction with no real `:fragment-height-mm` survey field still gets
  a real answer via `quarryops.cad`'s own disclosed formula-based
  default."
  [extraction]
  (if extraction
    (/ (:height-mm (cad/envelope-dims-mm extraction)) 2000.0)
    default-working-height-m))

(defn motion-plan-for
  "Ordered Cartesian waypoint list, one per `quarryops.robotics/
  mission-actions` entry (same order, same `:step` names):

    [{:seq :step :station :waypoint [x y z] :tool-orientation [dx dy dz]} ...]

  x = (action-index) * `station-pitch-m`; y = 0 (line centerline); z =
  `working-height-m`. `:seq` is 1-based (first action = seq 1).
  Deterministic: the same `extraction` always produces the same plan --
  `quarryops.robotics/mission-actions` is itself a fixed list and no
  randomness is introduced here. See ns docstring for why this plans a
  route through the INSPECTION ROBOT's verification mission, not the
  physics-simulated fragment's own free-fall trajectory."
  [& [extraction]]
  (let [z (working-height-m extraction)]
    (mapv (fn [i {:keys [step]}]
            {:seq (inc i) :step step :station (name step)
             :waypoint [(* i station-pitch-m) 0.0 z]
             :tool-orientation default-tool-orientation})
          (range (count robotics/mission-actions))
          robotics/mission-actions)))
