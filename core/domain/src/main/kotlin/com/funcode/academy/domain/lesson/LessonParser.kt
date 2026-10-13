package com.funcode.academy.domain.lesson

import com.funcode.academy.domain.lesson.json.ExerciseJson
import com.funcode.academy.domain.lesson.json.LessonIndexJson
import com.funcode.academy.domain.lesson.json.LessonJson
import com.funcode.academy.domain.lesson.json.ModuleJson
import com.funcode.academy.domain.lesson.json.SectionJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Índice del contenido: rutas relativas de módulos y de lecciones. */
data class ContentIndex(val modules: List<String>, val lessons: List<String>)

sealed interface ModuleParseResult {
    data class Valid(val definition: ModuleDefinition) : ModuleParseResult
    data class Invalid(val errors: List<String>) : ModuleParseResult
}

/** Resultado de leer una lección: o una lección válida, o la lista completa de problemas. */
sealed interface LessonParseResult {
    data class Valid(val lesson: Lesson) : LessonParseResult
    data class Invalid(val errors: List<String>) : LessonParseResult
}

/**
 * Convierte el JSON de una lección en un [Lesson] validado. Nunca lanza excepciones
 * por contenido incorrecto: devuelve [LessonParseResult.Invalid] con todos los
 * problemas encontrados, para que la app muestre un error controlado y los autores
 * sepan qué corregir.
 */
object LessonParser {

    const val SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json { ignoreUnknownKeys = false }
    private val ID_PATTERN = Regex("[a-z0-9]+([.-][a-z0-9]+)*")
    private val BLANK_PATTERN = Regex("""\{\{(\d+)}}""")

    fun parse(text: String): LessonParseResult {
        val raw = try {
            json.decodeFromString<LessonJson>(text)
        } catch (e: SerializationException) {
            return LessonParseResult.Invalid(listOf("JSON no válido: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return LessonParseResult.Invalid(listOf("JSON no válido: ${e.message}"))
        }
        return Builder(raw).build()
    }

    /** Lee el índice del contenido. Lanza [IllegalArgumentException] si no es válido. */
    fun parseIndex(text: String): ContentIndex {
        val index = try {
            json.decodeFromString<LessonIndexJson>(text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Índice no válido: ${e.message}", e)
        }
        require(index.schemaVersion == SUPPORTED_SCHEMA_VERSION) { "Versión de índice no compatible: ${index.schemaVersion}" }
        return ContentIndex(index.modules.orEmpty(), index.lessons.orEmpty())
    }

    /** Lee la definición de un módulo (las referencias a lecciones se comprueban en [ModuleAssembler]). */
    fun parseModule(text: String): ModuleParseResult {
        val raw = try {
            json.decodeFromString<ModuleJson>(text)
        } catch (e: SerializationException) {
            return ModuleParseResult.Invalid(listOf("JSON no válido: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return ModuleParseResult.Invalid(listOf("JSON no válido: ${e.message}"))
        }
        val errors = mutableListOf<String>()
        if (raw.schemaVersion != SUPPORTED_SCHEMA_VERSION) errors += "schemaVersion debe ser $SUPPORTED_SCHEMA_VERSION"
        val id = raw.id
        if (id == null || !ID_PATTERN.matches(id)) errors += "id de módulo no válido: '$id'"
        val order = raw.order
        if (order == null || order < 1) errors += "order debe ser un entero ≥ 1"
        val status = ReviewStatus.entries.firstOrNull { it.name == raw.reviewStatus }
        if (status == null) errors += "reviewStatus debe ser DRAFT o APPROVED"
        if (raw.title.isNullOrBlank()) errors += "Falta el campo 'title'"
        if (raw.description.isNullOrBlank()) errors += "Falta el campo 'description'"
        val lessons = raw.lessons.orEmpty()
        if (lessons.isEmpty()) errors += "El módulo necesita al menos una lección"
        if (errors.isNotEmpty()) return ModuleParseResult.Invalid(errors)
        return ModuleParseResult.Valid(
            ModuleDefinition(id!!, order!!, raw.title!!, raw.description!!, status!!, lessons, raw.assessment),
        )
    }

    private class Builder(private val raw: LessonJson) {
        private val errors = mutableListOf<String>()

        fun build(): LessonParseResult {
            if (raw.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                errors += "schemaVersion debe ser $SUPPORTED_SCHEMA_VERSION (recibido: ${raw.schemaVersion})"
            }
            val id = id("id", raw.id)
            val moduleId = id("moduleId", raw.moduleId)
            val languageId = text("languageId", raw.languageId)
            val locale = text("locale", raw.locale)
            val title = text("title", raw.title)
            val description = text("description", raw.description)
            val version = raw.version
            if (version == null || version < 1) errors += "version debe ser un entero ≥ 1"
            val status = ReviewStatus.entries.firstOrNull { it.name == raw.reviewStatus }
            if (status == null) errors += "reviewStatus debe ser DRAFT o APPROVED (recibido: ${raw.reviewStatus})"
            val kind = when (raw.kind) {
                null, "lesson" -> LessonKind.LESSON
                "assessment" -> LessonKind.ASSESSMENT
                else -> {
                    errors += "kind debe ser 'lesson' o 'assessment' (recibido: ${raw.kind})"
                    LessonKind.LESSON
                }
            }

            val objectives = objectives()
            val objectiveIds = objectives.map { it.id }.toSet()
            val presentations = presentations()
            val exercises = exercises(objectiveIds)
            objectives.forEach { objective ->
                if (exercises.none { objective.id in it.objectiveIds }) {
                    errors += "El objetivo '${objective.id}' no tiene ningún ejercicio que lo practique"
                }
            }
            val assessment = assessment(exercises.map { it.id }.toSet())
            if (kind == LessonKind.ASSESSMENT) {
                if (assessment == null) {
                    errors += "Una evaluación necesita el bloque 'assessment'"
                } else if (assessment.exerciseIds.toSet() != exercises.map { it.id }.toSet()) {
                    errors += "En una evaluación, assessment.exerciseIds debe incluir todos sus ejercicios"
                }
                exercises.filter { it.hints.isNotEmpty() }.forEach {
                    errors += "${it.id}: los ejercicios de una evaluación no pueden tener pistas"
                }
            }

            if (errors.isNotEmpty()) return LessonParseResult.Invalid(errors.toList())
            return LessonParseResult.Valid(
                Lesson(
                    id = id!!, moduleId = moduleId!!, languageId = languageId!!, locale = locale!!,
                    version = version!!, reviewStatus = status!!, kind = kind, title = title!!, description = description!!,
                    objectives = objectives, presentations = presentations, exercises = exercises,
                    assessment = assessment,
                ),
            )
        }

        private fun text(field: String, value: String?): String? {
            if (value.isNullOrBlank()) {
                errors += "Falta el campo '$field' o está vacío"
                return null
            }
            return value
        }

        private fun id(field: String, value: String?): String? {
            val v = text(field, value) ?: return null
            if (!ID_PATTERN.matches(v)) {
                errors += "'$field' = '$v' no es un identificador válido (minúsculas, números, '.' y '-')"
                return null
            }
            return v
        }

        private fun objectives(): List<LearningObjective> {
            val list = raw.objectives.orEmpty()
            if (list.isEmpty()) errors += "La lección necesita al menos un objetivo de aprendizaje"
            val result = list.mapIndexedNotNull { i, o ->
                val oid = id("objectives[$i].id", o.id)
                val text = text("objectives[$i].text", o.text)
                if (oid != null && text != null) LearningObjective(oid, text) else null
            }
            duplicates(result.map { it.id }).forEach { errors += "Objetivo duplicado: '$it'" }
            return result
        }

        private fun presentations(): Map<PresentationVariant, List<Section>> {
            val map = raw.presentations.orEmpty()
            val result = mutableMapOf<PresentationVariant, List<Section>>()
            map.forEach { (key, sections) ->
                val variant = PresentationVariant.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                if (variant == null) {
                    errors += "Variante de presentación desconocida: '$key'"
                    return@forEach
                }
                if (sections.isEmpty()) errors += "La variante '$key' no tiene secciones"
                result[variant] = sections.mapIndexedNotNull { i, s -> section("presentations.$key[$i]", s) }
            }
            if (PresentationVariant.DEFAULT !in result) errors += "Falta la presentación 'default' (obligatoria)"
            return result
        }

        private fun section(path: String, s: SectionJson): Section? = when (s.type) {
            "text" -> text("$path.text", s.text)?.let { Section.Text(it) }
            "callout" -> text("$path.text", s.text)?.let { Section.Callout(it) }
            "code" -> text("$path.code", s.code)?.let { code ->
                val lineCount = code.lines().size
                val comments = s.lineComments.orEmpty().mapIndexedNotNull { i, c ->
                    val comment = text("$path.lineComments[$i].comment", c.comment)
                    val line = c.line
                    if (line == null || line !in 1..lineCount) {
                        errors += "$path.lineComments[$i].line debe estar entre 1 y $lineCount"
                        null
                    } else {
                        comment?.let { LineComment(line, it) }
                    }
                }
                Section.Code(code, comments)
            }
            else -> {
                errors += "$path.type desconocido: '${s.type}' (válidos: text, code, callout)"
                null
            }
        }

        private fun exercises(objectiveIds: Set<String>): List<Exercise> {
            val list = raw.exercises.orEmpty()
            if (list.isEmpty()) errors += "La lección necesita al menos un ejercicio"
            val result = list.mapIndexedNotNull { i, e -> exercise("exercises[$i]", e, objectiveIds) }
            duplicates(result.map { it.id }).forEach { errors += "Ejercicio duplicado: '$it'" }
            return result
        }

        private fun exercise(path: String, e: ExerciseJson, objectiveIds: Set<String>): Exercise? {
            val errorsBefore = errors.size
            val eid = id("$path.id", e.id)
            val p = if (eid != null) "$path ($eid)" else path
            val prompt = text("$p.prompt", e.prompt)
            val explanation = text("$p.explanation", e.explanation)
            val incorrect = text("$p.incorrectFeedback", e.incorrectFeedback)
            val difficulty = e.difficulty
            if (difficulty == null || difficulty !in 1..5) errors += "$p.difficulty debe estar entre 1 y 5"
            val objectives = e.objectiveIds.orEmpty()
            if (objectives.isEmpty()) errors += "$p.objectiveIds no puede estar vacío"
            objectives.filterNot { it in objectiveIds }.forEach { errors += "$p hace referencia a un objetivo inexistente: '$it'" }
            val hints = e.hints.orEmpty()
            if (hints.any { it.isBlank() }) errors += "$p.hints contiene pistas vacías"
            val content = content(p, e)
            content?.let { checkHintsDoNotRevealAnswer(p, hints, it) }
            if (errors.size > errorsBefore) return null
            return Exercise(eid!!, objectives, difficulty!!, prompt!!, content!!, hints, explanation!!, incorrect!!)
        }

        private fun content(p: String, e: ExerciseJson): ExerciseContent? = when (e.type) {
            "multipleChoice" -> {
                val options = e.options.orEmpty()
                val correct = e.correctIndices.orEmpty().toSet()
                if (options.size < 2) errors += "$p necesita al menos 2 opciones"
                options.forEachIndexed { i, o -> text("$p.options[$i].text", o.text) }
                if (correct.isEmpty()) errors += "$p.correctIndices no puede estar vacío"
                if (correct.any { it !in options.indices }) errors += "$p.correctIndices contiene índices fuera de rango"
                if (correct.size == options.size && options.isNotEmpty()) errors += "$p: no puede ser correcta toda opción"
                ExerciseContent.MultipleChoice(options.map { ChoiceOption(it.text.orEmpty(), it.feedback) }, correct)
            }
            "trueFalse" -> {
                val statement = text("$p.statement", e.statement)
                if (e.correct == null) errors += "$p.correct es obligatorio (true o false)"
                if (statement != null && e.correct != null) ExerciseContent.TrueFalse(statement, e.correct) else null
            }
            "fillInBlanks" -> {
                val template = text("$p.template", e.template)
                val blanks = e.blanks.orEmpty()
                val markers = template?.let { BLANK_PATTERN.findAll(it).map { m -> m.groupValues[1].toInt() }.toList() }.orEmpty()
                if (blanks.isEmpty()) errors += "$p.blanks no puede estar vacío"
                if (markers != blanks.indices.toList()) {
                    errors += "$p.template debe contener los marcadores {{0}}…{{${blanks.size - 1}}} una vez y en orden"
                }
                if (blanks.any { b -> b.isEmpty() || b.any { it.isBlank() } }) errors += "$p.blanks: cada hueco necesita respuestas no vacías"
                template?.let { ExerciseContent.FillInBlanks(it, blanks) }
            }
            "orderLines" -> {
                val lines = e.lines.orEmpty()
                if (lines.size < 2) errors += "$p.lines necesita al menos 2 líneas"
                if (lines.any { it.isBlank() }) errors += "$p.lines contiene líneas vacías"
                val alternatives = e.alternativeOrders.orEmpty()
                if (alternatives.any { it.sorted() != lines.indices.toList() }) {
                    errors += "$p.alternativeOrders debe contener permutaciones de todas las líneas"
                }
                ExerciseContent.OrderLines(lines, alternatives)
            }
            "predictOutput" -> {
                val code = text("$p.code", e.code)
                val outputs = e.acceptedOutputs.orEmpty()
                if (outputs.isEmpty() || outputs.any { it.isBlank() }) errors += "$p.acceptedOutputs necesita al menos una salida no vacía"
                code?.let { ExerciseContent.PredictOutput(it, outputs) }
            }
            "findError" -> {
                val code = text("$p.code", e.code)
                val errorLines = e.errorLines.orEmpty().toSet()
                val lineCount = code?.lines()?.size ?: 0
                if (errorLines.isEmpty()) errors += "$p.errorLines no puede estar vacío"
                if (errorLines.any { it !in 1..lineCount }) errors += "$p.errorLines debe estar entre 1 y $lineCount"
                code?.let { ExerciseContent.FindError(it, errorLines) }
            }
            "matchPairs" -> {
                val left = e.left.orEmpty()
                val right = e.right.orEmpty()
                val pairs = e.pairs.orEmpty()
                if (left.size < 2 || left.size != right.size) errors += "$p.left y $p.right deben tener el mismo tamaño (≥ 2)"
                if (left.any { it.isBlank() } || right.any { it.isBlank() }) errors += "$p contiene conceptos vacíos"
                val map = pairs.filter { it.size == 2 }.associate { it[0] to it[1] }
                val bijective = pairs.all { it.size == 2 } && map.keys == left.indices.toSet() &&
                    map.values.toSet() == right.indices.toSet() && map.size == pairs.size
                if (!bijective) errors += "$p.pairs debe asociar cada elemento de 'left' con uno distinto de 'right'"
                ExerciseContent.MatchPairs(left, right, map)
            }
            else -> {
                errors += "$p.type desconocido: '${e.type}' (válidos: multipleChoice, trueFalse, fillInBlanks, " +
                    "orderLines, predictOutput, findError, matchPairs)"
                null
            }
        }

        /** Una pista nunca debe ser, literalmente, la respuesta. */
        private fun checkHintsDoNotRevealAnswer(p: String, hints: List<String>, content: ExerciseContent) {
            val answers: List<String> = when (content) {
                is ExerciseContent.PredictOutput -> content.acceptedOutputs
                is ExerciseContent.FillInBlanks -> content.acceptedAnswers.flatten().filter { it.length > 2 }
                else -> emptyList()
            }
            val normalizedAnswers = answers.map { CodeText.normalizeOutput(it) }
            hints.forEachIndexed { i, hint ->
                if (normalizedAnswers.any { a -> a.isNotEmpty() && CodeText.normalizeOutput(hint).contains(a) }) {
                    errors += "$p.hints[$i] revela la respuesta"
                }
            }
        }

        private fun assessment(exerciseIds: Set<String>): Assessment? {
            val a = raw.assessment ?: return null
            val ids = a.exerciseIds.orEmpty()
            if (ids.isEmpty()) errors += "assessment.exerciseIds no puede estar vacío"
            ids.filterNot { it in exerciseIds }.forEach { errors += "assessment hace referencia a un ejercicio inexistente: '$it'" }
            // El umbral es opcional: mientras no esté aprobado (P6) no se incluye.
            val threshold = a.passThreshold
            if (threshold != null && (threshold <= 0.0 || threshold > 1.0)) {
                errors += "assessment.passThreshold debe estar entre 0 (excluido) y 1"
            }
            return Assessment(ids, threshold)
        }

        private fun duplicates(values: List<String>) = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    }
}
