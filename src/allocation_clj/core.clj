(ns allocation_clj.core
  (use [korma.db]
       [korma.core]
       )
  (import [org.jclouds.abiquo.domain.infrastructure Datacenter Rack Machine]
          [org.jclouds.abiquo  AbiquoContextFactory]
          [java.util Properties]))


(defdb kinton {:subprotocol "mysql"
          :subname "//localhost:3306/kinton"
          :user "root"
          :password "root"
          :db "kinton"
          :delimiters "`"})

(def allocate_sql "select if(count(n.id) < (r.vlan_id_max - r.vlan_id_min + 1) - round(((r.vlan_id_max - r.vlan_id_min + 1) * r.nrsq) / 100) ,ifnull((select case when exists ( select ne.idEnterprise from virtualmachine ne where ne.idEnterprise in ( ( select ee.idEnterprise2 from workload_enterprise_exclusion_rule ee where ee.idEnterprise1 = 1 ) ) ) then -3 when exists ( select ne.idEnterprise from virtualmachine ne where ne.idEnterprise in ( ( select ee.idEnterprise1 from workload_enterprise_exclusion_rule ee where ee.idEnterprise2 = 1 ) ) ) then -3 when exists ( select l.id from workload_machine_load_rule l where if(exists (select ll.idMachine from workload_machine_load_rule ll where ll.idMachine = m.idPhysicalMachine), l.idMachine = m.idPhysicalMachine, if(exists (select ll.idRack from workload_machine_load_rule ll where ll.idRack in (select mm.idRack from physicalmachine mm where m.idPhysicalMachine = m.idPhysicalMachine ) and ll.idMachine is null), l.idRack in (select mm.idRack from physicalmachine mm where mm.idPhysicalMachine = m.idPhysicalMachine and l.idMachine is null), l.idDatacenter in (select mm.idDataCenter from physicalmachine mm where mm.idPhysicalMachine = m.idPhysicalMachine and l.idRack is null and l.idMachine is null))) and ((l.cpuLoadPercentage * m.cpu / 100) <= (m.cpuUsed + (select vm.cpu from virtualmachine vm where vm.idVM = 1))) and ((l.ramLoadPercentage * m.ram / 100) <= (m.ramUsed + (select vm.ram from virtualmachine vm where vm.idVM = 1)))) then -4 else m.idPhysicalMachine end from physicalmachine m inner join hypervisor hyper on m.idPhysicalMachine = hyper.idPhysicalMachine inner join virtualdatacenter vdc on hyper.type = vdc.hypervisorType inner join datastore_assignment store_a on store_a.idPhysicalMachine = m.idPhysicalMachine inner join datastore store on store.idDatastore = store_a.idDatastore where vdc.idVirtualDataCenter = 4 and m.idRack = 2 and m.idState = 3 and (m.idEnterprise is NULL or m.idEnterprise = 1) and store.enabled = true and store.size > store.usedSize and ( store.size - store.usedSize ) > ( select n.limitResource * 1024 * 1024 + ne.hd from rasd_management m inner join rasd n on m.idResource = n.instanceID inner join virtualmachine ne on m.idVM = ne.idVM where ne.idVM = 1 and m.idResourceType = 17 ) order by (case when 'PROGRESSIVE' = (select f.fitPolicy from workload_fit_policy_rule f where f.idDatacenter = vdc.idDatacenter or f.idDatacenter order by f.idDatacenter desc limit 1) then -((m.cpu - m.cpuUsed) + ((m.ram - m.ramUsed) / 512) * 100 / 2) else ((m.cpu - m.cpuUsed) + ((m.ram - m.ramUsed) / 512) * 100 / 2) end) desc, size desc limit 1) , -2) ,-1) \"idPhysicalMachine\" from vlan_network_assignment n inner join rack r on n.idRack = r.idRack where n.idRack = ?")

(def VALID-CHARS
  (map char (concat (range 48 58) ; 0-9
                    (range 65 91) ; A-Z
                    (range 97 123)))) ; a-z

(def login "admin")
(def credential "xabiquo")

(def props (doto (java.util.Properties.) (.putAll {"abiquo.endpoint" "http://localhost/api" "abiquo.contextbuilder" "org.jclouds.abiquo.AbiquoContextBuilder" "abiquo.propertiesbuilder" "org.jclouds.abiquo.AbiquoPropertiesBuilder"})))
(def context (. (AbiquoContextFactory.) (createContext login credential props)))


(def dcs (..  context (getAdministrationService) (listDatacenters)))

(defn- create
  []
  (let [dcbuilder (. (Datacenter/builder context) (name "Morgul Mines") (location "morgul") (remoteServices "10.60.1.224" "ENTERPRISE"))
        dc (. dcbuilder (build))]
          (. dc (save))))
  

(defn random-char []
  (rand-nth VALID-CHARS))
(defn random-str [length]
  (apply str (take length (repeatedly random-char))))
(defn create-racks
  []
  (doseq [n dcs] 
    (let [rbuilder (Rack/builder context n)]
                     (doseq [i (range 0 1000)]
                                  (. rbuilder (name (random-str 17)))
                                   (let [r (. rbuilder (build))]
                                      (. r (save)))))))
(defn -main
  [& args]
  (doseq [i (range 0 1)]
    (println (exec-raw [allocate_sql [2]] :results)))
  (println (. dcs (size)))
  (if (< (. dcs (size)) 1)
    (do (create)
    (create-racks)))
  (let [rr (first dcs)
        r (first (. rr (listRacks)))
        mbuilder (. (Machine/builder context r) (name (random-str 10)) (virtualRamInMb 1024) (virtualCpuCores 4) (ip "1.1.1.1") (port 80) (state "MANAGED") (user "root") (password "temporal"))
        m (. mbuilder (build))  ]
             (. m (save))))
  
