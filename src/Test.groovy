import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import java.text.SimpleDateFormat
/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

//实体类的包名,不配置则根据选着目录
packageName = "com.guoyw"
typeMapping = [
        (~/(?i)tinyint|smallint|mediumint/)      : "Integer",
        (~/(?i)int/)                             : "Long",
        (~/(?i)bool|bit/)                        : "Boolean",
        (~/(?i)float|double|decimal|real/)       : "BigDecimal",
        (~/(?i)datetime|timestamp|date|time/)    : "Date",
        (~/(?i)date|datetime|timestamp/)         : "java.util.Date",
        (~/(?i)time/)                            : "java.sql.Time",
        (~/(?i)/)                                : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
   /* dir = dir + "\\entity"
    def file1 = new File(dir)
    if(!file1.isDirectory()){
        file1.mkdir()
    }*/

    def path =java.lang.String.valueOf(dir)  + "\\entity"
    def file1 = new File(path)
    if(!file1.isDirectory()){
        file1.mkdir()
    }

    def file = new File(path,"test.txt")

    file << "\n dir : " + dir
    file << "\n path : " + path
    file << "\n str : " + "\\entity"
}

// 生成实体类
def generateEntity(table, dir){
    def className = javaName(table.getName(), true)
    packageName = getPackageName(dir)
    new File(dir,className+"Entity.java").withPrintWriter {
        out-> writerEntity(out, table)
    }
}

// 写实体类
def writerEntity(out, table){
    def className = javaName(table.getName(), true)
    className = className + "Entity"
    def fields = calcFields(table)
    def date = new Date().format("yyyy/MM/dd")


    // --- start 开始写
    out.println "package $packageName"

    out.println "import lombok.Data;"     // 因为我使用了lombok插件，使用到了Data注解，所以在引包时加了这一行
    out.println "import lombok.experimental.Accessors;"     // 返回this
    out.println "import io.swagger.annotations.ApiModelProperty;"   // 同上，使用了swagger文档，所以引入到需要的注解
    out.println "import javax.persistence.Id;"  // tk.mybatis插件需用时需要@id注解，所以引入，不需要就去掉
    out.println "import java.io.Serializable;"
    out.println "import java.util.Date;"


    // 写入类注释信息
    out.println ""
    out.println "/**"
    out.println " * Created on $date."
    out.println " * @author guoyw"
    out.println " */"

    //写入实体信息
    out.println "@Data"
    out.println "@Accessors(chain = true)"
    out.println "@Entity"
    out.println "@Table ( name =\"" + table.getName() + "\" )"
    out.println "public class $className implements Serializable {"
    out.println ""

    int i = 0
    fields.each() {   // 遍历字段，按下面的规则生成
        // 输出注释，这里唯一的是id特殊判断了一下，如果判断it.name == id, 则多添加一行@Id
        if (it.name == "id") {
            if (!isNotEmpty(it.commoent)) {
                out.println "\t/**"
                out.println "\t * 主键id"
                out.println "\t */"
                out.println "\t@ApiModelProperty(value = \"主键id\", position = ${i})"
            }
            out.println "\t@Id"
        }
        if (isNotEmpty(it.commoent)) {
            out.println "\t/**"
            out.println "\t * ${it.commoent}"
            out.println "\t */"
            out.println "\t@ApiModelProperty(value = \"${it.commoent}\", position = ${i})"
        }
        if (it.annos != "") out.println "  ${it.annos}"
        out.println "\tprivate ${it.type} ${it.name};"
        out.println ""
        i++
    }
    out.println ""
    out.println "}"

}

// 生成get 和 set方法
def generateGetAndSet(out,fields){
    // 输出get/set方法
    fields.each() {
        out.println ""
        out.println "  public ${it.type} get${it.name.capitalize()}() {"
        out.println "    return ${it.name};"
        out.println "  }"
        out.println ""
        out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
        out.println "    this.${it.name} = ${it.name};"
        out.println "  }"
        out.println ""
    }
}

// 生成toString
def generateToString(out,fields){
    out.println "\t@Override"
    out.println "\tpublic String toString() {"
    out.println "\t\treturn \"{\" +"
    fields.each() {
        out.println "\t\t\t\t\t\"${it.name}='\" + ${it.name} + '\\'' +"
    }
    out.println "\t\t\t\t'}';"
    out.println "\t}"
    out.println ""
}

// 获取表字段信息
def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           comment: col.getComment(),
                           colName: col.getName(),
                           name   : javaName(col.getName(), false),
                           type   : typeStr,
                           annos  : "@Column(name = \"" + col.getName() + "\" )"]]
    }
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
def getPackageName(dir) {
  return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}