import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */
//生成的 service 所在包位置，不配置则根据选着目录
packageName = "**;"
FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    // 组合路径,判断目录是否存在，不存在这创建
    def path =java.lang.String.valueOf(dir)  + "\\service"
    def file = new File(path)
    if(!file.isDirectory()){
        file.mkdir()
    }
    packageName = getPackageName(dir,"service")
    generateService(table, path)
}

def generateService(table, path) {
    def baseName = javaName(table.getName(), true)

    def file = new File(path, baseName + "Service.java")
    //文件存在则直接退出
    if(file.exists()){
     return
    }

    file.withPrintWriter {
        out -> generateInterface(out, baseName)
    }
}

def generateInterface(out, baseName) {
    def date = new Date().format("yyyy/MM/dd")
    def entityPackage = packageName.replaceAll("service;","entity")
    def repositoryPackage = packageName.replaceAll("service;","repository")

    out.println "package $packageName"
    out.println "import ${entityPackage}.${baseName}Entity;"
    out.println "import ${repositoryPackage}.${baseName}Repository;"
    out.println "import org.springframework.stereotype.Service;"
    out.println "import com.yioks.core.base.repository.UuidBaseJpaRepository;"
    out.println "import org.springframework.beans.factory.annotation.Autowired;"

    // 写入类注释信息
    out.println ""
    out.println "/**"
    out.println " * Created on $date."
    out.println " * @author guoyw"
    out.println " */"

    out.println "@Service"
    out.println "public class ${baseName}Service extends UuidBaseJpaService<${baseName}Entity> {" // 可自定义
    out.println ""
    out.println "\t@Autowired"
    out.println "\tprivate ${baseName}Repository repository;"
    out.println ""
    out.println "\t@Override"
    out.println "\tprotected UuidBaseJpaRepository<${baseName}Entity>  getRepository(){ return repository; }"
    out.println "}"
}

// 获取字段名称，下划线转驼峰
def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
      .collect { Case.LOWER.apply(it).capitalize() }
      .join("")
      .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    name = capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

//判断是否为空
def isNotEmpty(content) {
    return content != null && content.toString().trim().length() > 0
}

// 获取包所在文件夹路径
def getPackageName(dir,type) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "")+"."+type+ ";"
}