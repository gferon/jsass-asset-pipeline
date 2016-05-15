/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package asset.pipeline.jsass

import asset.pipeline.AbstractProcessor
import asset.pipeline.AssetCompiler
import asset.pipeline.AssetFile
import asset.pipeline.AssetPipelineConfigHolder
import asset.pipeline.fs.FileSystemAssetResolver
import groovy.util.logging.Log4j
import io.bit3.jsass.CompilationException
import io.bit3.jsass.Compiler
import io.bit3.jsass.Options

@Log4j
class SassProcessor extends AbstractProcessor {
    final Compiler compiler = new Compiler();
    final Options options = new Options();

    SassProcessor(AssetCompiler precompiler) {
        super(precompiler)
        options.getImporters().add(new SassCacheManagerImporter())
        // TODO: Implement more options
        if(!precompiler) {
            if (AssetPipelineConfigHolder.config?.sass?.sourceComments) {
                options.setSourceComments(true)
                options.setSourceMapEmbed(true)
            }
        }
    }

    /**
     * Called whenever the asset pipeline detects a change in the file provided as argument
     * TODO: Find a way to resolve the full filepath without accessing the private resolver field
     * @param input the content of the SCSS file to compile
     * @param assetFile
     * @return
     */
    String process(String input, AssetFile assetFile) {
        try {
            if (assetFile.getSourceResolver() instanceof FileSystemAssetResolver) {
                def baseDir = ((FileSystemAssetResolver) assetFile.getSourceResolver()).baseDirectory
                def inputFileName = new File(baseDir.canonicalPath, assetFile.path)

                if (!this.precompiler) {
                    SassCacheManagerImporter.baseDirectoryThreadLocal.set(baseDir.absolutePath)
                    SassCacheManagerImporter.assetFileThreadLocal.set(assetFile)
                }

                log.info "Compiling $assetFile.name"
                // TODO: Find a better way to use the current working directory?
                def output = compiler.compileString(input, inputFileName.toURI(), null, options)
                return output.css
            } else {
                throw new IOException('Failed to resolve base directory of assets pipeline')
            }
        } catch (CompilationException e) {
            log.error(e.errorMessage)
            throw e
        }
    }
}