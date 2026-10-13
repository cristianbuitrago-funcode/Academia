package com.funcode.academy.domain.mission

import com.funcode.academy.domain.lesson.LessonParser
import com.funcode.academy.domain.lesson.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionParserTest {

    private fun json(
        type: String = "learning",
        prerequisites: String = "[]",
        tasks: String = """[{"kind":"completeLesson","lessonId":"m.l1"}]""",
        extra: String = "",
    ) = """{"schemaVersion":1,"id":"m.mi01","moduleId":"m","order":1,"version":1,"reviewStatus":"DRAFT","type":"$type",
        "title":"T","description":"D","objective":"O","prerequisites":$prerequisites,"tasks":$tasks$extra}"""

    private fun errors(text: String) = (LessonParser.parseMission(text) as MissionParseResult.Invalid).errors

    @Test fun `mision valida con todos los tipos de tarea`() {
        val text = json(
            type = "practice",
            prerequisites = """["m.mi00"]""",
            tasks = """[{"kind":"completeLesson","lessonId":"m.l1"},
                {"kind":"solveExercises","exerciseIds":["m.l1.e1","m.l1.e2"],"description":"Resuelve"},
                {"kind":"passAssessment","assessmentId":"m.eval"},
                {"kind":"completeChallenge","challengeId":"m.reto"}]""",
        )
        val mission = (LessonParser.parseMission(text) as MissionParseResult.Valid).mission
        assertEquals(
            MissionDefinition(
                "m.mi01", "m", 1, 1, ReviewStatus.DRAFT, MissionType.PRACTICE, "T", "D", "O", listOf("m.mi00"),
                listOf(
                    MissionTask.CompleteLesson("m.l1"),
                    MissionTask.SolveExercises(listOf("m.l1.e1", "m.l1.e2"), "Resuelve"),
                    MissionTask.PassAssessment("m.eval"),
                    MissionTask.CompleteChallenge("m.reto"),
                ),
            ),
            mission,
        )
    }

    @Test fun `los cuatro tipos de mision`() {
        listOf("learning", "practice", "assessment", "project").forEach {
            assertTrue(it, LessonParser.parseMission(json(type = it)) is MissionParseResult.Valid)
        }
    }

    @Test fun `tipo y tarea desconocidos`() {
        assertTrue(errors(json(type = "aventura")).any { "type" in it })
        assertTrue(errors(json(tasks = """[{"kind":"bailar"}]""")).any { "kind" in it })
    }

    @Test fun `mision incompleta`() {
        val e = errors("""{"schemaVersion":1,"id":"m.mi01","reviewStatus":"DRAFT","type":"learning","tasks":[]}""")
        listOf("moduleId", "order", "version", "'title'", "'description'", "'objective'", "al menos una tarea").forEach { f ->
            assertTrue(f, e.any { f in it })
        }
    }

    @Test fun `tareas mal formadas`() {
        val e = errors(json(tasks = """[{"kind":"solveExercises","exerciseIds":["a","a"]},{"kind":"completeLesson"}]"""))
        assertTrue(e.any { "repite" in it })
        assertTrue(e.any { "description" in it })
        assertTrue(e.any { "lessonId" in it })
        assertTrue(errors(json(tasks = """[{"kind":"solveExercises","exerciseIds":[],"description":"D"}]""")).any { "vacío" in it })
    }

    @Test fun `una mision no puede requerirse a si misma`() =
        assertTrue(errors(json(prerequisites = """["m.mi01"]""")).any { "sí misma" in it })

    @Test fun `json roto o campos desconocidos`() {
        assertTrue(errors("{ roto").any { "JSON" in it })
        assertTrue(errors(json(extra = ""","xp":999""")).any { "JSON" in it })
    }
}
