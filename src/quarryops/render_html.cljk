(ns quarryops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave1 Lane A-hand): drives the REAL actor stack
  (`quarryops.operation` -> `quarryops.governor` -> `quarryops.store`)
  through a scenario adapted from this repo's own `quarryops.sim` demo
  driver (`clojure -M:dev:run`, confirmed against real seeded
  extraction ids `extraction-1`..`extraction-7` that match
  `quarryops.store/demo-data`), trimmed to a representative subset
  (one full intake->assess->robot-verify->extract->ship lifecycle, and
  four distinct HARD-hold reasons) and rendered deterministically --
  no invented numbers, no timestamps in the page content,
  byte-identical across reruns against the same seed (verify by
  diffing two consecutive runs).

  Styling follows the 9522/`applianceshop.render-html` reference:
  `jp-go-dds.skin/dds+skin` (デジタル庁デザインシステム + skin).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [kotoba.lang.text :as str]
            [quarryops.store :as store]
            [quarryops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :quarry-operator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing dispositions
  this actor can reach: extraction-1 clears a full lifecycle --
  intake (auto-commit clean at phase 3, no capital risk), a
  jurisdiction assessment (phase-gated -- not yet auto-eligible --
  approved), a robot quarry-face verification mission (approved), a
  material extraction (ALWAYS escalates -- `:actuation/extract-material`
  is permanently high-stakes, never auto at any phase -- approved) and
  a consignment shipment (ALWAYS escalates -- `:actuation/ship-consignment`,
  same posture -- approved); extraction-2 HARD-holds a jurisdiction
  assessment with no official spec-basis for its (deliberately
  unregistered) jurisdiction; extraction-3 clears its own jurisdiction
  assessment (approved) but then HARD-holds a material extraction whose
  claimed royalty (150.0) doesn't match the independently recomputed
  quantity x royalty-rate (20 x 5.0 = 100.0); extraction-4 HARD-holds
  an extraction whose permit is invalid; extraction-5 HARD-holds a
  blasting extraction with unconfirmed blast-safety clearance. Every
  HARD hold never reaches a human. Returns the resulting store -- every
  field read by `render` below is real governor/store output, not a
  hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    ;; --- extraction-1: full clean lifecycle ---
    (exec! actor "t1-intake" {:op :extraction/intake :subject "extraction-1"
                               :patch {:id "extraction-1" :site "North Face"}})

    (exec! actor "t1-assess" {:op :jurisdiction/assess :subject "extraction-1"})
    (approve! actor "t1-assess")

    (exec! actor "t1-robot" {:op :robotics/simulate-quarry-face-verification
                             :subject "extraction-1"})
    (approve! actor "t1-robot")

    (exec! actor "t1-extract" {:op :extraction/extract :subject "extraction-1"})
    (approve! actor "t1-extract")

    (exec! actor "t1-ship" {:op :consignment/ship :subject "extraction-1"})
    (approve! actor "t1-ship")

    ;; --- HARD holds (robot-verify first where needed so each row
    ;; surfaces a DISTINCT hard rule, not the shared robotics-missing) ---
    (exec! actor "t2-assess" {:op :jurisdiction/assess :subject "extraction-2"
                              :no-spec? true})

    (exec! actor "t3-assess" {:op :jurisdiction/assess :subject "extraction-3"})
    (approve! actor "t3-assess")
    (exec! actor "t3-robot" {:op :robotics/simulate-quarry-face-verification
                             :subject "extraction-3"})
    (approve! actor "t3-robot")
    (exec! actor "t3-extract" {:op :extraction/extract :subject "extraction-3"})

    (exec! actor "t4-assess" {:op :jurisdiction/assess :subject "extraction-4"})
    (approve! actor "t4-assess")
    (exec! actor "t4-robot" {:op :robotics/simulate-quarry-face-verification
                             :subject "extraction-4"})
    (approve! actor "t4-robot")
    (exec! actor "t4-extract" {:op :extraction/extract :subject "extraction-4"})

    (exec! actor "t5-assess" {:op :jurisdiction/assess :subject "extraction-5"})
    (approve! actor "t5-assess")
    (exec! actor "t5-robot" {:op :robotics/simulate-quarry-face-verification
                             :subject "extraction-5"})
    (approve! actor "t5-robot")
    (exec! actor "t5-extract" {:op :extraction/extract :subject "extraction-5"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger extraction-id]
  (last (filter #(= (:subject %) extraction-id) ledger)))

(defn- status-cell [ledger extraction-id]
  (let [f (last-fact-for ledger extraction-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (or (-> f :violations first :rule)
                     (-> f :basis first))]
        (str "<span class=\"critical\">HARD hold &middot; "
             (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- lifecycle-cell [{:keys [extracted? shipped?]}]
  (cond
    shipped? "<span class=\"ok\">extracted &amp; shipped</span>"
    extracted? "<span class=\"warn\">extracted, not yet shipped</span>"
    :else "<span class=\"muted\">in progress</span>"))

(defn- extraction-row [ledger {:keys [id site material-type jurisdiction] :as e}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc site) (esc (name (or material-type :n-a)))
          (esc jurisdiction)
          (lifecycle-cell e)
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", "))
                   (some-> disposition name) ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own closed op contract
  ;; (README `Ops`, `quarryops.governor`/`quarryops.phase`) --
  ;; documentation of fixed behavior, not runtime telemetry, so it is
  ;; legitimately hand-described rather than derived from a live run.
  ["        <tr><td><code>:extraction/intake</code></td><td><span class=\"ok\">phase-3 auto-commit when clean, no capital risk</span></td></tr>"
   "        <tr><td><code>:jurisdiction/assess</code></td><td><span class=\"warn\">phase-3: human approval (not yet auto-eligible) &middot; HARD hold on missing spec-basis</span></td></tr>"
   "        <tr><td><code>:robotics/simulate-quarry-face-verification</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; real physics-2d bench-face sim</span></td></tr>"
   "        <tr><td><code>:extraction/extract</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; independent royalty recompute + permit + blast-safety + robotics-sim checks</span></td></tr>"
   "        <tr><td><code>:consignment/ship</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; already-shipped re-checked</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        extractions (store/all-extractions db)
        extraction-rows (str/join "\n" (map (partial extraction-row ledger) extractions))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-0810 &middot; quarrying</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Quarrying of stone, sand and clay (ISIC 0810) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · extract/ship always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Extractions</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>quarryops.store</code> via <code>quarryops.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Extraction</th><th>Site</th><th>Material</th><th>Jurisdiction</th><th>Extract/ship status</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     extraction-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Quarry Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. Royalties are independently recomputed, never trusted from the proposal; a material extraction is blocked outright on a missing robot verification mission, out-of-tolerance physics-2d recheck, invalid permit, or unconfirmed blast-safety clearance on blasting work.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Extraction</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)
        out-file (java.io.File. out)]
    (some-> (.getParentFile out-file) .mkdirs)
    (spit out-file html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/extraction-history db)) "material extractions,"
             (count (store/shipment-history db)) "consignment shipments )")))
