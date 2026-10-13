package com.funcode.academy.domain.lesson

import com.funcode.academy.domain.lesson.TestLessons.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonParserTest {

    private fun errorsOf(text: String): List<String> {
        val result = LessonParser.parse(text)
        assertTrue("Se esperaba contenido inválido", result is LessonParseResult.Invalid)
        return (result as LessonParseResult.Invalid).errors
    }

    private fun assertError(text: String, fragment: String) {
        val errors = errorsOf(text)
        assertTrue("Ningún error contiene «$fragment»: $errors", errors.any { fragment in it })
    }

    @Test fun `una leccion valida se carga completa`() {
        val result = LessonParser.parse(json())
        assertTrue(result is LessonParseResult.Valid)
        val lesson = (result as LessonParseResult.Valid).lesson
        assertEquals("t.l1", lesson.id)
        assertEquals(ReviewStatus.DRAFT, lesson.reviewStatus)
        assertEquals(1, lesson.exercises.size)
        assertEquals(ExerciseContent.TrueFalse("S", true), lesson.exercises.single().content)
    }

    @Test fun `una variante que falta usa la presentacion por defecto`() {
        val lesson = (LessonParser.parse(json()) as LessonParseResult.Valid).lesson
        assertEquals(listOf(Section.Text("Teoría")), lesson.sectionsFor(com.funcode.academy.domain.model.Audience.ADULTS))
    }

    @Test fun `JSON mal formado`() = assertError("{ esto no es json", "JSON no válido")

    @Test fun `campos desconocidos se rechazan para detectar erratas`() = assertError(json(extra = ""","tittle":"x""""), "JSON no válido")

    @Test fun `faltan campos obligatorios y se informan todos a la vez`() {
        val errors = errorsOf("""{"schemaVersion":1}""")
        assertTrue(errors.any { "'id'" in it })
        assertTrue(errors.any { "'title'" in it })
        assertTrue(errors.any { "objetivo" in it })
        assertTrue(errors.any { "ejercicio" in it })
        assertTrue(errors.any { "default" in it })
    }

    @Test fun `version de esquema no compatible`() = assertError(json().replace("\"schemaVersion\":1", "\"schemaVersion\":2"), "schemaVersion")

    @Test fun `estado de revision desconocido`() = assertError(json().replace("DRAFT", "LISTO"), "reviewStatus")

    @Test fun `identificador con formato no valido`() = assertError(json().replace("\"t.l1\"", "\"Lección 1\""), "identificador válido")

    @Test fun `tipo de ejercicio desconocido`() = assertError(json().replace("\"trueFalse\"", "\"dibujar\""), "type desconocido")

    @Test fun `referencia a un objetivo inexistente`() = assertError(json().replace("[\"o1\"],\"difficulty\"", "[\"o9\"],\"difficulty\""), "objetivo inexistente")

    @Test fun `dificultad fuera de rango`() = assertError(json().replace("\"difficulty\":1", "\"difficulty\":7"), "difficulty")

    @Test fun `seccion de codigo con comentario fuera de rango`() = assertError(
        json(presentations = """{"default":[{"type":"code","code":"a\nb","lineComments":[{"line":3,"comment":"c"}]}]}"""),
        "lineComments[0].line",
    )

    @Test fun `ejercicios duplicados`() {
        val e = """{"id":"t.e1","type":"trueFalse","objectiveIds":["o1"],"difficulty":1,"prompt":"P","statement":"S",
            "correct":true,"explanation":"E","incorrectFeedback":"I"}"""
        assertError(json(exercises = "[$e,$e]"), "Ejercicio duplicado")
    }

    private fun exercise(fields: String) = json(
        exercises = """[{"id":"t.e1","objectiveIds":["o1"],"difficulty":1,"prompt":"P","explanation":"E",
            "incorrectFeedback":"I",$fields}]""",
    )

    @Test fun `seleccion multiple con indice fuera de rango`() =
        assertError(exercise(""""type":"multipleChoice","options":[{"text":"a"},{"text":"b"}],"correctIndices":[2]"""), "fuera de rango")

    @Test fun `huecos que no coinciden con los marcadores`() =
        assertError(exercise(""""type":"fillInBlanks","template":"x {{0}} {{2}}","blanks":[["a"],["b"]]"""), "marcadores")

    @Test fun `parejas que no forman una correspondencia uno a uno`() = assertError(
        exercise(""""type":"matchPairs","left":["a","b"],"right":["x","y"],"pairs":[[0,0],[1,0]]"""),
        "pairs",
    )

    @Test fun `lineas de error fuera del codigo`() =
        assertError(exercise(""""type":"findError","code":"a\nb","errorLines":[5]"""), "errorLines")

    @Test fun `orden alternativo que no es una permutacion`() =
        assertError(exercise(""""type":"orderLines","lines":["a","b"],"alternativeOrders":[[0,0]]"""), "alternativeOrders")

    @Test fun `una pista que revela la salida esperada se rechaza`() = assertError(
        exercise(""""type":"predictOutput","code":"c","acceptedOutputs":["Hola"],"hints":["Escribe Hola"]"""),
        "revela la respuesta",
    )

    @Test fun `un objetivo sin ejercicios se rechaza`() =
        assertError(json().replace("[{\"id\":\"o1\",\"text\":\"O\"}]", "[{\"id\":\"o1\",\"text\":\"O\"},{\"id\":\"o2\",\"text\":\"O2\"}]"), "'o2'")

    @Test fun `evaluacion con umbral no valido o ejercicio inexistente`() {
        val errors = errorsOf(json(extra = ""","assessment":{"exerciseIds":["t.e9"],"passThreshold":1.5}"""))
        assertTrue(errors.any { "t.e9" in it })
        assertTrue(errors.any { "passThreshold" in it })
    }

    @Test fun `indice de modulos y lecciones`() = assertEquals(
        ContentIndex(listOf("modules/m.json"), listOf("lessons/a.json")),
        LessonParser.parseIndex("""{"schemaVersion":1,"modules":["modules/m.json"],"lessons":["lessons/a.json"]}"""),
    )

    @Test(expected = IllegalArgumentException::class)
    fun `un indice mal formado se informa como argumento no valido`() {
        LessonParser.parseIndex("{ roto")
    }

    @Test fun `una leccion es de tipo leccion por defecto`() =
        assertEquals(LessonKind.LESSON, (LessonParser.parse(json()) as LessonParseResult.Valid).lesson.kind)

    @Test fun `tipo de leccion desconocido`() = assertError(json(extra = ""","kind":"examen""""), "kind")

    @Test fun `una evaluacion necesita el bloque assessment`() = assertError(json(extra = ""","kind":"assessment""""), "bloque 'assessment'")

    @Test fun `una evaluacion debe incluir todos sus ejercicios y no tener pistas`() {
        val errors = errorsOf(
            json(extra = ""","kind":"assessment","assessment":{"exerciseIds":[]}"""),
        )
        assertTrue(errors.any { "todos sus ejercicios" in it })
        assertTrue(errors.any { "no pueden tener pistas" in it })
    }

    @Test fun `una evaluacion valida sin umbral`() {
        val text = json(
            exercises = """[{"id":"t.e1","type":"trueFalse","objectiveIds":["o1"],"difficulty":1,"prompt":"P",
                "statement":"S","correct":true,"explanation":"E","incorrectFeedback":"I"}]""",
            extra = ""","kind":"assessment","assessment":{"exerciseIds":["t.e1"]}""",
        )
        val lesson = (LessonParser.parse(text) as LessonParseResult.Valid).lesson
        assertEquals(LessonKind.ASSESSMENT, lesson.kind)
        assertEquals(null, lesson.assessment!!.passThreshold)
    }

    @Test fun `modulo valido`() {
        val r = LessonParser.parseModule(
            """{"schemaVersion":1,"id":"t.m1","order":1,"reviewStatus":"DRAFT","title":"T","description":"D",
                "lessons":["t.l1"],"assessment":"t.eval"}""",
        )
        assertEquals(
            ModuleParseResult.Valid(ModuleDefinition("t.m1", 1, "T", "D", ReviewStatus.DRAFT, listOf("t.l1"), "t.eval")),
            r,
        )
    }

    @Test fun `modulo sin lecciones ni orden`() {
        val r = LessonParser.parseModule("""{"schemaVersion":1,"id":"t.m1","reviewStatus":"DRAFT","title":"T","description":"D"}""")
        val errors = (r as ModuleParseResult.Invalid).errors
        assertTrue(errors.any { "order" in it })
        assertTrue(errors.any { "lección" in it })
    }

    @Test fun `un reto se lee con su tipo y permite pistas`() {
        val lesson = (LessonParser.parse(json(extra = ""","kind":"challenge"""")) as LessonParseResult.Valid).lesson
        assertEquals(LessonKind.CHALLENGE, lesson.kind)
    }

    @Test fun `un reto no puede tener bloque assessment`() =
        assertError(json(extra = ""","kind":"challenge","assessment":{"exerciseIds":["t.e1"]}"""), "Un reto")

    @Test fun `modulo con reto`() {
        val r = LessonParser.parseModule(
            """{"schemaVersion":1,"id":"t.m1","order":1,"reviewStatus":"DRAFT","title":"T","description":"D",
                "lessons":["t.l1"],"challenge":"t.reto"}""",
        )
        assertEquals("t.reto", (r as ModuleParseResult.Valid).definition.challengeId)
    }
}
