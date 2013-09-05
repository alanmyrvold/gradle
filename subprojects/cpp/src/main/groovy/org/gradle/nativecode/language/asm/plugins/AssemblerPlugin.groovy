/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativecode.language.asm.plugins

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.nativecode.base.NativeBinary
import org.gradle.nativecode.base.ToolChainTool
import org.gradle.nativecode.base.internal.NativeBinaryInternal
import org.gradle.nativecode.base.plugins.NativeBinariesPlugin
import org.gradle.nativecode.language.asm.AssemblerSourceSet
import org.gradle.nativecode.language.asm.tasks.Assemble
import org.gradle.nativecode.toolchain.plugins.GppCompilerPlugin
import org.gradle.nativecode.toolchain.plugins.MicrosoftVisualCppPlugin

/**
 * A plugin for projects wishing to build native binary components from Assembly language sources.
 *
 * <p>Automatically includes the {@link AssemblerLangPlugin} for core Assembler support and the {@link NativeBinariesPlugin} for native binary support,
 * together with the {@link MicrosoftVisualCppPlugin} and {@link GppCompilerPlugin} for core toolchain support.</p>
 *
 * <li>Creates a {@link Assemble} task for each {@link AssemblerSourceSet} to assemble the sources.</li>
 */
@Incubating
class AssemblerPlugin implements Plugin<ProjectInternal> {

    void apply(ProjectInternal project) {
        project.plugins.apply(NativeBinariesPlugin)
        project.plugins.apply(MicrosoftVisualCppPlugin)
        project.plugins.apply(GppCompilerPlugin)

        project.plugins.apply(AssemblerLangPlugin)

        // TODO:DAZ Clean this up
        project.executables.all {
            it.binaries.all {
                ext.assembler = new ToolChainTool()
            }
        }
        project.libraries.all {
            it.binaries.all {
                ext.assembler = new ToolChainTool()
            }
        }

        project.binaries.withType(NativeBinary) { NativeBinaryInternal binary ->
            binary.source.withType(AssemblerSourceSet).all { AssemblerSourceSet sourceSet ->
                def compileTask = createAssembleTask(project, binary, sourceSet)
                binary.builderTask.source compileTask.outputs.files.asFileTree.matching { include '**/*.obj', '**/*.o' }
            }
        }
    }

    private def createAssembleTask(ProjectInternal project, NativeBinaryInternal binary, def sourceSet) {
        def assembleTask = project.task(binary.namingScheme.getTaskName("assemble", sourceSet.fullName), type: Assemble) {
            description = "Assembles the $sourceSet sources of $binary"
        }

        assembleTask.toolChain = binary.toolChain

        assembleTask.source sourceSet.source

        assembleTask.conventionMapping.objectFileDir = { project.file("${project.buildDir}/objectFiles/${binary.namingScheme.outputDirectoryBase}/${sourceSet.fullName}") }
        assembleTask.assemblerArgs = binary.assembler.args

        assembleTask
    }

}