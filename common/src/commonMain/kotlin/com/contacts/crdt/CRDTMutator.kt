package com.contacts.crdt

import com.tap.hlc.HybridLogicalClock

typealias CRDTMutator = TableBackedCRDT.() -> LinkedHashSet<(HybridLogicalClock) -> CRDTDelta>
