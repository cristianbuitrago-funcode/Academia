package com.funcode.academy.domain.mission

import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonSummary

/** Misiones válidas y los problemas de las que se han descartado. */
data class MissionSet(val missions: List<MissionDefinition>, val errors: List<String>)

/**
 * Comprueba que cada misión solo hace referencia a contenido existente y coherente,
 * y que sus requisitos existen y no forman ciclos. Una misión inválida se descarta, y
 * también las que dependen de ella (si no, quedarían bloqueadas para siempre).
 */
object MissionAssembler {

    fun assemble(definitions: List<MissionDefinition>, lessons: Map<String, LessonSummary>): MissionSet {
        val errors = mutableListOf<String>()
        val exerciseOwners = lessons.values
            .filter { it.kind != LessonKind.ASSESSMENT }
            .flatMap { lesson -> lesson.exerciseIds.map { it to lesson } }.toMap()

        definitions.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            .forEach { errors += "La misión '$it' está repetida" }
        val unique = definitions.distinctBy { it.id }
        val invalid = mutableSetOf<String>()

        unique.forEach { m ->
            fun lesson(id: String, kind: LessonKind): Boolean {
                val lesson = lessons[id]
                val problem = when {
                    lesson == null -> "hace referencia a una actividad inexistente o inválida: '$id'"
                    lesson.kind != kind -> "'$id' debe ser de tipo $kind"
                    lesson.moduleId != m.moduleId -> "'$id' pertenece a otro módulo (${lesson.moduleId})"
                    else -> null
                }
                problem?.let { errors += "Misión '${m.id}': $it" }
                return problem == null
            }
            val tasksOk = m.tasks.map { task ->
                when (task) {
                    is MissionTask.CompleteLesson -> lesson(task.lessonId, LessonKind.LESSON)
                    is MissionTask.PassAssessment -> lesson(task.assessmentId, LessonKind.ASSESSMENT)
                    is MissionTask.CompleteChallenge -> lesson(task.challengeId, LessonKind.CHALLENGE)
                    is MissionTask.SolveExercises -> task.exerciseIds.map { id ->
                        val owner = exerciseOwners[id]
                        val problem = when {
                            owner == null -> "el ejercicio '$id' no existe en ninguna lección o reto"
                            owner.moduleId != m.moduleId -> "el ejercicio '$id' pertenece a otro módulo"
                            else -> null
                        }
                        problem?.let { errors += "Misión '${m.id}': $it" }
                        problem == null
                    }.all { it }
                }
            }.all { it }
            val unknownPrerequisites = m.prerequisiteIds.filter { p -> unique.none { it.id == p } }
            unknownPrerequisites.forEach { errors += "Misión '${m.id}': requisito inexistente '$it'" }
            if (!tasksOk || unknownPrerequisites.isNotEmpty()) invalid += m.id
        }

        // Ciclos de requisitos.
        val byId = unique.associateBy { it.id }
        fun inCycle(start: String): Boolean {
            val seen = mutableSetOf<String>()
            fun visit(id: String): Boolean {
                if (id == start && seen.isNotEmpty()) return true
                if (!seen.add(id)) return false
                return byId[id]?.prerequisiteIds.orEmpty().any { visit(it) }
            }
            return visit(start)
        }
        unique.filter { inCycle(it.id) }.forEach {
            errors += "Misión '${it.id}': sus requisitos forman un ciclo"
            invalid += it.id
        }

        // Las que dependen de una misión descartada también se descartan.
        var changed = true
        while (changed) {
            changed = false
            unique.filter { it.id !in invalid && it.prerequisiteIds.any { p -> p in invalid } }.forEach {
                errors += "Misión '${it.id}': depende de una misión no válida"
                invalid += it.id
                changed = true
            }
        }
        return MissionSet(unique.filter { it.id !in invalid }.sortedWith(compareBy({ it.moduleId }, { it.order })), errors)
    }
}
