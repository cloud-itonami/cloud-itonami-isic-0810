(ns quarryops.render-html
  "Build-time HTML renderer. Drives the REAL actor stack deterministically.
   Usage: clojure -M:dev:render-html [out-file]."
  (:require [clojure.string :as str]
            [quarryops.store :as store]
            [quarryops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator {:actor-id "op-1" :actor-role :quarry-operator :phase 3})
(defn- exec! [actor tid request] (g/run* actor {:request request :context operator} {:thread-id tid}))
(defn- approve! [actor tid] (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn run-demo! []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "t1" {:op :extraction/intake :subject "extraction-1"
                       :patch {:id "extraction-1" :site "North Face"}})
    (exec! actor "t2" {:op :jurisdiction/assess :subject "extraction-1"})
    (approve! actor "t2")
    (exec! actor "t2b" {:op :robotics/simulate-quarry-face-verification :subject "extraction-1"})
    (approve! actor "t2b")
    (exec! actor "t3" {:op :extraction/extract :subject "extraction-1"})
    (approve! actor "t3")
    (exec! actor "t4" {:op :consignment/ship :subject "extraction-1"})
    (approve! actor "t4")
    (exec! actor "t5" {:op :jurisdiction/assess :subject "extraction-2" :no-spec? true})
    db))

(defn- esc [v]
  (-> (str v) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))
(defn- last-fact-for [ledger eid] (last (filter #(= (:subject %) eid) ledger)))
(defn- status-cell [ledger eid]
  (let [f (last-fact-for ledger eid)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :basis first)]
        (str "<span class=\"critical\">HARD hold: " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))
(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", ")) (some-> disposition name) ""))))
(def ^:private action-gate-rows
  ["        <tr><td><code>:extraction/intake</code></td><td><span class=\"ok\">auto-commit when clean</span></td></tr>"
   "        <tr><td><code>:jurisdiction/assess</code></td><td><span class=\"warn\">ALWAYS human approval; spec-basis required</span></td></tr>"
   "        <tr><td><code>:robotics/simulate-quarry-face-verification</code></td><td><span class=\"warn\">ALWAYS human approval (quarry-face survey)</span></td></tr>"
   "        <tr><td><code>:extraction/extract</code></td><td><span class=\"warn\">ALWAYS human approval; royalty recompute + permit + blasting checks</span></td></tr>"
   "        <tr><td><code>:consignment/ship</code></td><td><span class=\"warn\">ALWAYS human approval (actuation)</span></td></tr>"])
(defn render [db]
  (let [ledger (vec (store/ledger db))
        extractions (->> (store/all-extractions db)
                         (filter #(#{"extraction-1" "extraction-2"} (:id %)))
                         (sort-by :id))
        ext-row (fn [e]
                  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
                          (esc (:id e)) (esc (or (:site e) "-"))
                          (esc (str (or (:material-type e) :n-a))) (status-cell ledger (:id e))))
        ext-rows (str/join "\n" (map ext-row extractions))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-0810</title>"
     "<style>body{font:14px/1.5 sans-serif;margin:0;color:#1a1a1a;background:#f5f5f5}"
     ".bar{background:#3a2a0a;color:#fff;padding:1.2rem 2rem}.bar h1{margin:0;font-size:1.15rem}"
     "main{max-width:980px;margin:1.5rem auto;padding:0 1rem}"
     ".card{background:#fff;border-radius:8px;padding:1.2rem 1.4rem;margin-bottom:1.2rem;box-shadow:0 1px 3px rgba(0,0,0,.08)}"
     ".muted{color:#777;font-size:.82rem}table{border-collapse:collapse;width:100%;font-size:.85rem}"
     "th,td{text-align:left;padding:.42rem .5rem;border-bottom:1px solid #eee}th{font-weight:600;color:#555}"
     ".ok{color:#0a7d33}.warn{color:#9a6700}.critical{color:#b41010;font-weight:600}"
     "code{background:#f0f0f0;padding:.1rem .3rem;border-radius:3px;font-size:.8rem}</style></head><body>"
     "<header class=\"bar\"><h1>Quarrying ops (ISIC 0810) — <code>quarryops</code></h1></header><main>"
     "<section class=\"card\"><h2>Extractions</h2>"
     "<p class=\"muted\">Demo from <code>quarryops.store</code> via <code>quarryops.render-html</code>. No invented data.</p>"
     "<table><thead><tr><th>Extraction</th><th>Site</th><th>Material</th><th>Status</th></tr></thead><tbody>"
     ext-rows "</tbody></table></section>"
     "<section class=\"card\"><h2>Action gate</h2>"
     "<table><thead><tr><th>Op</th><th>Gate</th></tr></thead><tbody>"
     (str/join "\n" action-gate-rows) "</tbody></table></section>"
     "<section class=\"card\"><h2>Audit ledger</h2>"
     "<table><thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead><tbody>"
     ledger-rows "</tbody></table></section></main></body></html>")))
(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!) out-file (java.io.File. out)]
    (.. out-file getParentFile mkdirs)
    (spit out-file (render db))
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts )")))
