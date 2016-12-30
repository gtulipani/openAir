# openAir
Proyecto enteramente académico para explorar la tecnología Selenium con Java , a través de un ejemplo práctico para cargar las horas en el sitio OpenAir. Algunas características tratadas:
- WebElements a través de id, name, xpath
- Listas e iteradores.
- Logging de actividad.
- shutDownHook: bloque de código dónde se almacena lo último que se ejecuta cuando se llama al método System.exit(int i).
- Reflection: un ejemplo práctico dónde se buscan variables en tiempo de ejecución de acuerdo a su nombre, y se modifica su valor.

#Consideraciones
Para que el programa funcione, debe existir un archivo de texto llamado "openAir.conf", que almacene las claves que se utilizarán a lo largo del programa. Debe tener el siguiente formato por linea:
<CLAVE>=<VALUE>. Las claves son las siguientes: COMPANY, USER, PASSWORD, CHARGE_CODE, DATE, CHROME_DRIVER. Ejemplo:

COMPANY=directv
USER=gtulipani
PASSWORD=password
CHARGE_CODE=XXXX1111
DATE=12/30/16
CHROME_DRIVER=C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe

#Archivos en el repositorio
A modo de ejemplo, se encuentra un archivo llamado "openAir.conf" y otro llamado "chromedriver.exe" en el repositorio, los cuales no se descargan automáticamente (añadidos al .gitignore). El segundo es el software necesario para que funcione la automatización en Chrome.
