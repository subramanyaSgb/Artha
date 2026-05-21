package com.subramanya.artha.data.db.seed

import com.subramanya.artha.domain.rules.RuleSpecJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedRulesTest {

    @Test fun `seed produces exactly the 10 PRD rules`() {
        val rules = SeedRules.all()
        assertEquals("Expected 10 pre-seeded rules from PRD §10", 10, rules.size)
    }

    @Test fun `every seeded rule has a stable id and decodes cleanly`() {
        val ids = mutableSetOf<String>()
        for (rule in SeedRules.all()) {
            assertTrue("rule id must start with 'rule_': ${rule.id}", rule.id.startsWith("rule_"))
            assertTrue("rule id must be unique: ${rule.id}", ids.add(rule.id))
            assertTrue("seed rules are system rules", rule.isSystem)
            assertTrue("seed rules ship active by default", rule.isActive)

            val conditions = RuleSpecJson.decodeConditions(rule.conditionsJson)
            assertNotNull(conditions)
            assertTrue("rule ${rule.id} has no conditions decoded", conditions.items.isNotEmpty())

            val actions = RuleSpecJson.decodeActions(rule.actionsJson)
            assertTrue("rule ${rule.id} has no actions decoded", actions.items.isNotEmpty())
        }
    }
}
