package asset.pipeline.jsass

import asset.pipeline.AssetFile
import asset.pipeline.AssetHelper
import asset.pipeline.CacheManager
import groovy.util.logging.Log4j
import io.bit3.jsass.importer.Import
import io.bit3.jsass.importer.Importer

import java.nio.file.Path
import java.nio.file.Paths

@Log4j
class SassCacheManagerImporter implements Importer {
    static final ThreadLocal<String> baseDirectoryThreadLocal = new ThreadLocal();
    static final ThreadLocal<AssetFile> assetFileThreadLocal = new ThreadLocal();

    /**
     * Find the real file name to be resolved to a AssetFile instance
     * libsass returns full absolute path, so we need to resolve this to a path relative
     * to the baseDirectory of the asset pipeline
     * @param url
     * @return
     */
    static AssetFile getAssetFromScssImport(String root, String parent, String importUrl) {
        Path rootPath = Paths.get(root)
        Path parentPath = Paths.get(parent)
        Path relativeRootPath = rootPath.relativize(parentPath.parent)

        // Searching for the real file, this is necessary as libsass normalize the imported filename
        Path importUrlPath = Paths.get(importUrl)
        def possibleStylesheets = ["${importUrlPath}.scss", "${importUrlPath.parent ? importUrlPath.parent.toString() + '/' : ''}_${importUrlPath.fileName}.scss"]
        for(String possibleStylesheet: possibleStylesheets) {
            Path stylesheetPath = relativeRootPath.resolve(possibleStylesheet)
            def potentialAssetFile = AssetHelper.fileForFullName(stylesheetPath.toString())
            if(potentialAssetFile) {
                return potentialAssetFile
            }
        }

        throw new UnsupportedOperationException("Unable to find the asset for $importUrl")
    }

    @Override
    public Collection<Import> apply(String url, Import previous) {
        try {
            def baseDirectory = baseDirectoryThreadLocal.get()
            def assetFile = assetFileThreadLocal.get()
            def dependantAsset = getAssetFromScssImport(baseDirectory, previous.absoluteUri.toString(), url)
            if (assetFile && dependantAsset) {
                log.debug "$assetFile.name depends on $dependantAsset.path"
                CacheManager.addCacheDependency(assetFile.name, dependantAsset)
            } else {
                log.warn "Cannot solve dependencies for root $assetFile: $url imported by $previous.absoluteUri"
            }
        } catch (e) {
            log.error "Failed to add a dependency for $url imported by $previous.importUri", e
        }
        return null;
    }
}
