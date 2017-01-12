import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by gaston.tulipani on 26/12/2016.
 */
public class Main {
    //Enum con las claves que se leeran en el confFile. Si se agrega un nueva key, debe agregarse el correspondiente parámetro, con el mismo nombre, pero en lowerCase
    private enum ConfKey {
        OPENAIR,
        COMPANY,
        USER,
        PASSWORD,
        CHARGE_CODE,
        COMPANY_PROJECT_CODE,
        DATE,
        CHROME_DRIVER,
        TYPE,
        HOURS;
    }

    //confFile parameters
    private static String openair = "";
    private static String company = "";
    private static String user =  "";
    private static String password = "";
    private static String charge_code = "";
    private static String company_project_code = "";
    private static String date = "";
    private static String chrome_driver = "";
    private static String type = "";
    private static String hours = "";

    //Log related constants
    private final static String LOG_FILE = "activity.log";
    private final static String LOG_INIT = "Comienzo de la ejecución del programa.";
    private final static String LOG_CREATION = "Creación del archivo de Log.";
    private final static String LOG_CLOSING = "Fin de la ejecución del programa.";

    //confFile related constants
    private final static char CONF_FILE_SEPARATOR = '=';
    private final static String CONF_FILE_NAME = "openAir.conf";
    private final static String CONF_FILE_NOT_FOUND = "Archivo " + CONF_FILE_NAME + " no encontrado. Debe ubicarse en la misma carpeta que el archivo ejecutable.";
    private final static String KEY_NOT_VALID = "Se ha detectado una clave no válida en el archivo " + CONF_FILE_NAME + ". La misma ha sido omitida.";
    private final static String KEY_NOT_FOUND = "Se ha detectado una línea del archivo " + CONF_FILE_NAME + " que no contiene ninguna clave.  La misma ha sido omitida. Las claves y los valores se dividen a través del caracter " + CONF_FILE_SEPARATOR + ".";
    private final static String JVM_NO_PERMISSION_REFLECTION = "La JVM no permite el uso de Reflection. Modifique este comportamiento para que el programa funcione.";

    private final static String OPEN_AIR_TIMESHEET_CREADA = "Se ha creado la Timesheet correspondiente a la fecha " + date + ".";
    private final static String OPEN_AIR_TIMESHEET_SUBMITTEADA = "Se ha submitteado la Timesheet correspondiente a la fecha " + date + " con el " + ConfKey.CHARGE_CODE.name() + "= \"" + charge_code + "\"";
    private final static String OPEN_AIR_SCREENSHOT_NO_CREADA = "No se ha podido crear la screenshot correspondiente para el día " + date + ".";
    private final static String OPEN_AIR_SCREENSHOT_CREADA = "Se ha creado la screenshot correspondiente para el día " + date + ".";
    private final static String OPEN_AIR_SCREENSHOT_WAIT_ERROR = "Ha ocurrido un error al momento de sacar la screenshot. Puede sacarla ahora y oprimir ENTER una vez haya finalizado...";
    private final static String OPEN_AIR_SCREENSHOT_WAIT_SUCCEED = "Una vez que termine de cargar la página oprima ENTER y se tomará la captura de pantalla inmediatamente.";
    private final static String OPEN_AIR_CARGA_NO_AUTOMÁTICA = "Usted ha elegido el método de carga no automática. Seleccione manualmente el/los códigos e ingrese las correspondientes horas. Una vez que ha terminado oprima ENTER y se continuará con el proceso.";

    //webDriver related constants
    private final static String DRIVER_INIT = "El driver del navegador ha sido cargado.";
    private final static String DRIVER_CLOSING = "El driver del navegador ha sido cerrado.";
    private final static String DRIVER_PATH_NOT_VALID = "El driver del navegador no puso ser cargado. Revise el valor de " + ConfKey.CHROME_DRIVER.name() + ".";
    private final static String URL_NOT_VALID = "La URL especificada para la aplicación: \"" + openair + "\" se encuentra mal conformada. Por favor revísela.";
    private final static String URL_NOT_REACHABLE = "La URL especificada para la aplicación: \"" + openair + "\" no se puede cargar. Revise la misma y la conexión a internet.";
    private final static String LOGIN_NOT_VALID = "El logueo en la aplicación no ha sido exitoso. Revise los valores de " + ConfKey.COMPANY.name() + ", " + ConfKey.USER.name() + " y " + ConfKey.PASSWORD.name() + ".";
    private final static String LOGIN_VALID = "El logueo en la aplicación ha sido exitoso.";
    private final static String DATE_NOT_VALID = "No se pudo encontrar la fecha especificada a través de la clave " + ConfKey.DATE.name() +  " en el selector al momento de creación de la timesheet. Revise la misma. Proceso abortado.";
    private final static String COMPANY_CODE_NOT_VALID = "No se pudo encontrar el company charge code especificado a través de la clave " + ConfKey.COMPANY_PROJECT_CODE.name() +  " en el selector al momento de creación de la timesheet. Revise la misma. Proceso abortado.";
    private final static String CHARGE_CODE_NOT_VALID = "No se pudo encontrar el charge code especificado a través de la clave " + ConfKey.CHARGE_CODE.name() +  " en el selector al momento de creación de la timesheet. Revise la misma. Proceso abortado.";
    private final static String TYPE_AUTOMATIC = "A";
    private final static String TYPE_MANUAL = "M";
    private final static String TYPE_NOT_VALID = "Debe elegir un modo de procesamiento válido a través de la clave " + ConfKey.TYPE.name() + ". Debe elegir el valor Y o N.";

    public static void main(String[] args) {
        File logFile = new File(LOG_FILE);
        logInitializer(logFile);

        //Preparo el Logueo en el programa para el caso que se cierre (normal o abruptamente)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logWriter(logFile, LOG_CLOSING);
            }
        });

        //Analizo el archivo confFile, de dónde extraigo todas las claves necesarias
        analizeConfFile(logFile);

        //Inicio el navegador Web
        WebDriver driver = webDriverInitializer(logFile);

        WebDriverWait wait = new WebDriverWait(driver, 30);

        //Realizo el proceso completo de carga de horas
        openAirProcess(driver, wait, logFile);
    }

    //Método que se encarga de analizar el archivo de configuración confFile
    private static void analizeConfFile(File logFile) {
        try {
            parseConfigurationFile(logFile);
        } catch (RutaDeArchivoNoValidaException e) {
            logWriter(logFile, CONF_FILE_NOT_FOUND);
            System.exit(1);
        }
        //Valido que se hayan parseado todas las claves
        keysValidator();
    }

    //Método que se encarga de iniciar el navegador Web
    private static WebDriver webDriverInitializer(File logFile) {
        WebDriver driver = null;
        try {
            driver = webDriverOpener(logFile);
        } catch (DriverPathNoValidoException e) {
            logWriter(logFile, DRIVER_PATH_NOT_VALID);
            System.exit(1);
        } catch (URLMalConformadaException e) {
            logWriter(logFile, URL_NOT_VALID);
            System.exit(1);
        } catch (NoInternetConnectionException e) {
            logWriter(logFile, URL_NOT_REACHABLE);
            System.exit(1);
        }
        driver.manage().window().maximize();
        return driver;
    }

    //Este método se encarga de obtener todos los elementos con el tag option, detectar aquel que empieza con el CHARGE_CODE leído y devolver el valor completo
    private static String obtenerNombreChargeCode(WebDriver driver) {
        List<WebElement> list = driver.findElements(By.tagName("option"));
        Iterator<WebElement> iterator = list.iterator();
        Boolean encontrado = false;
        String completeName = null;
        while (iterator.hasNext() && !encontrado) {
            WebElement element = iterator.next();
            if (element.getText().startsWith(charge_code))
            {
                completeName = element.getText();
                encontrado = true;
            }
        }
        if (!encontrado)
            throw new ChargeCodeNoEncontradoException();
        return completeName;
    }

    //Este método se encarga de realizar el nombre con el que se guardará la screenshot
    private static String obtenerNombreScreenshot() {
        Date fecha;
        try {
            fecha = new SimpleDateFormat("MM/dd/yy").parse(date);
        } catch (ParseException e) {
            throw new NoSePudoGenerarScreenshotException();
        }
        return new SimpleDateFormat("MM-dd-yy").format(fecha);
    }

    //Este método congela el funcionamiento hasta que el usuario oprima una tecla. Se utiliza para darle tiempo a sacar una screenshot en caso de error
    private static void esperarConfirmaciónDelUsuario(String cadena) {
        System.out.print(cadena);
        try {
            int caracterEspera = System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Método que se encarga de todo el proceso de openAir
    private static void openAirProcess(WebDriver driver, WebDriverWait wait, File logFile) {
        //Me logueo en la aplicación
        try {
            login(driver, wait, logFile);
        } catch (LoginNoValidoException e) {
            logWriter(logFile, LOGIN_NOT_VALID);
            closeDriver(driver, logFile);
            System.exit(1);
        }

        //Apreto el botón [+]

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("oa3_button_create_new")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("oa3_button_create_new")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("oa3_toolbox_create_new")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("oa3_toolbox_create_new")));
        driver.findElement(By.id("oa3_toolbox_create_new")).click();

        //Selecciono la opción Timesheets: Timesheet New
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("oa3_global_create_new")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("oa3_global_create_new")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"oa3_global_create_new\"]/a[1]")));
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"oa3_global_create_new\"]/a[1]")));
        driver.findElement(By.xpath("//*[@id=\"oa3_global_create_new\"]/a[1]")).click();

        //Selecciono la Timesheet starting date correspondiente. En teoría debería estar marcada por defecto, pero por las dudas.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"row_section_0\"]/table/tbody/tr[2]/td/select")));
        Select dateSelector = new Select(driver.findElement(By.xpath("//*[@id=\"row_section_0\"]/table/tbody/tr[2]/td/select")));
        try {
            dateSelector.selectByVisibleText(date);
        } catch (NoSuchElementException e) {
            logWriter(logFile, DATE_NOT_VALID);
            closeDriver(driver, logFile);
            System.exit(1);
        }

        //Oprimo el botón Save
        driver.findElement(By.xpath("//*[@id=\"formButtonsBottom\"]/input[2]")).click();
        logWriter(logFile, OPEN_AIR_TIMESHEET_CREADA);

        if (cargaAutomática()) {
            realizarCargaAutomática(driver, wait, logFile);
        }
        else {
            esperarConfirmaciónDelUsuario(OPEN_AIR_CARGA_NO_AUTOMÁTICA);
        }

        //Oprimo Save and Submit
        driver.findElement(By.id("save_grid_submit")).click();
        logWriter(logFile, OPEN_AIR_TIMESHEET_SUBMITTEADA);

        //Saco captura de pantalla para enviar por mail

        esperarConfirmaciónDelUsuario(OPEN_AIR_SCREENSHOT_WAIT_SUCCEED);
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, new File(obtenerNombreScreenshot() + ".jpg"));
        } catch (IOException | NoSePudoGenerarScreenshotException e) {
            logWriter(logFile, OPEN_AIR_SCREENSHOT_NO_CREADA);
            esperarConfirmaciónDelUsuario(OPEN_AIR_SCREENSHOT_WAIT_ERROR);
            closeDriver(driver, logFile);
            System.exit(0);
        }

        logWriter(logFile, OPEN_AIR_SCREENSHOT_CREADA);

        //Cierro el navegador
        closeDriver(driver, logFile);
    }

    private static boolean cargaAutomática() {
        boolean result;
        if (type.equals(TYPE_AUTOMATIC)) {
            result = true;
        }
        else {
            if (type.equals(TYPE_MANUAL)) {
                result = false;
            }
            //Ingresé un valor inválido
            else {
                char answer = '\0';
                System.out.println(TYPE_NOT_VALID);
                result = false;
                while (!respuestaValida(answer)) {
                    System.out.print("Especifique el tipo de operación 'A' (Automática) o 'M' (Manual): ");
                    answer = (new Scanner(System.in)).next().charAt(0);
                    if (!respuestaValida(answer)) {
                        System.out.println("La respuesta ha sido incorrecta. Recuerde ingresar 'A' o 'M'.");
                    }
                    else {
                        if (Character.toUpperCase(answer) == TYPE_AUTOMATIC.charAt(0)) {
                            result = true;
                        }
                    }
                }

            }
        }
        return result;
    }

    //Éste método valida si la respuesta ingresada por el usuario corresponde a un tipo
    private static boolean respuestaValida(char answer) {
        return (((Character.toUpperCase(answer) == TYPE_MANUAL.charAt(0)) || (Character.toUpperCase(answer) == TYPE_AUTOMATIC.charAt(0))));
    }

    //Éste método se encarga de realizar la carga en caso que sea automática
    private static void realizarCargaAutomática(WebDriver driver, WebDriverWait wait, File logFile) {
        //Completo el campo correspondiente a Company: Project
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ts_c2_r1")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ts_c2_r1")));
        Select companySelector = new Select(driver.findElement(By.id("ts_c2_r1")));
        try {
            companySelector.selectByVisibleText(company_project_code);
        } catch (NoSuchElementException e) {
            logWriter(logFile, COMPANY_CODE_NOT_VALID);
            closeDriver(driver, logFile);
            System.exit(1);
        }

        //Guardo todas las Task correspondientes al Project y elijo aquella que empieza con el CHARGE_CODE que obtuve
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ts_c3_r1")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ts_c3_r1")));
        Select taskSelector = new Select(driver.findElement(By.id("ts_c3_r1")));
        try {
            taskSelector.selectByVisibleText(obtenerNombreChargeCode(driver));
        } catch (ChargeCodeNoEncontradoException e) {
            logWriter(logFile, CHARGE_CODE_NOT_VALID);
            closeDriver(driver, logFile);
            System.exit(1);
        }

        //Completo los campos correspondientes con 8 horas
        driver.findElement(By.id("ts_c6_r1")).sendKeys(hours);
        driver.findElement(By.id("ts_c7_r1")).sendKeys(hours);
        driver.findElement(By.id("ts_c8_r1")).sendKeys(hours);
        driver.findElement(By.id("ts_c9_r1")).sendKeys(hours);
        driver.findElement(By.id("ts_c10_r1")).sendKeys(hours);
    }

    //Método que se encarga de cerrar el navegador y guardar el correspondiente mensaje en el archivo de Log
    private static void closeDriver(WebDriver driver, File logFile) {
        logWriter(logFile, DRIVER_CLOSING);
        driver.quit();
    }

    //Método que se encarga de transformar la fecha actual a String para imprimirse en el archivo de Log
    private static String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    //Método que se encarga de escribir el mensaje en el archivo logFile
    private static void logWriter(File logFile, String logMessage) {
        FileWriter fw = null;
        PrintWriter pw;
        try {
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw); //Append
            pw.println(getCurrentDate() + " - " + logMessage);
        } catch (IOException e) {
            throw new ArchivoNoPudoSerModificadoException();
        } finally {
            try {
                fw.close(); //Me aseguro de cerrar el archivo
            } catch (IOException e) {
                throw new ArchivoNoPudoSerModificadoException();
            }
        }
    }

    //Método que se asegura que exista el archivo de Log e inicializarlo
    private static void logInitializer(File logFile) {
        if (logFile.exists()) {
            logWriter(logFile, LOG_INIT);
        }
        else {
            logWriter(logFile, LOG_CREATION);
        }
    }

    /* Método que se encarga de modificar el campo @variable por el valor @claveValor a través de reflection
    ** @variable: Field que representa una variable
    ** @claveValor: String cuyo valor será guardado en @variable
     */
    private static void modificarVariableString(Field variable, String claveValor) {
        variable.setAccessible(true);

        try {
            variable.set((new Main()).getClass(), claveValor);
        } catch (IllegalAccessException e) {
            throw new NoPermissionOnJVMReflectionException();
        }

        variable.setAccessible(false);
    }

    //Método que se encarga de obtener una variable a través de su nombre a través del uso de reflection
    private static Field obtenerVariableDeNombre(String claveLeida) {
        Field field;
        Class<?> c = (new Main()).getClass();

        try {
            field = c.getDeclaredField(claveLeida);
        } catch (NoSuchFieldException e) {
            //Si la clave leida no tiene su correspondiente variable
            throw new ClaveNoValidaException();
        }
        return field;
    }

    //Método que se encarga de obtener el valor que almacena el Field @variable a través de reflection
    private static String obtenerValorDeVariable(Field variable) {
        String valorVariable;
        try {
            valorVariable = (String) variable.get((new Main()).getClass());
        } catch (IllegalAccessException e) {
            throw new NoPermissionOnJVMReflectionException();
        }
        return valorVariable;
    }

    //Método que se encarga de tomar la linea del confFile y obtener la clave que representa
    private static String parseKey(String linea) {
        String lineaCortada;
        try {
            lineaCortada = linea.substring(0, linea.indexOf(CONF_FILE_SEPARATOR));
            //String.indexOf() devuelve (-1) si no encuentra el caracter
            //String.substring() tira excepción si los límites son inválidos
        } catch (StringIndexOutOfBoundsException e) {
            throw new ClaveNoEncontradaException();
        }
        return lineaCortada;
    }

    //Método que se encarga de analizar línea del confFile y parsear las claves
    private static void parseLine(String linea) {
        String claveLeida = parseKey(linea);
        //Obtengo lo que se encuentra a la derecha del caracter separador
        String claveValor = linea.substring(ConfKey.valueOf(claveLeida).name().length() + 1);

        //Tener en cuenta que las variables se llaman de igual forma que las claves pero toda en minúsculas
        modificarVariableString(obtenerVariableDeNombre(claveLeida.toLowerCase()), claveValor);
    }

    //Método que se encarga de parsear el archivo de configuración y obtener los valores que se utilizaran en el programa
    private static void parseConfigurationFile(File logFile) {
        File fileToParse = new File(CONF_FILE_NAME);
        FileReader fr;
        try {
            fr = new FileReader(fileToParse);
        } catch (FileNotFoundException e) {
            throw new RutaDeArchivoNoValidaException();
        }

        BufferedReader br = new BufferedReader(fr);
        String linea;
        try {
            while((linea = br.readLine()) != null) {
                 try {
                     parseLine(linea);
                 } catch (ClaveNoValidaException e) {
                     logWriter(logFile, KEY_NOT_VALID);
                 } catch (ClaveNoEncontradaException e) {
                     logWriter(logFile, KEY_NOT_FOUND);
                 } catch (NoPermissionOnJVMReflectionException e) {
                     logWriter(logFile, JVM_NO_PERMISSION_REFLECTION);
                 }
            }
        } catch (IOException e) {
            System.out.print("hola");
        } finally {
            try {
                fr.close(); //Me aseguro de cerrar el archivo
            } catch (IOException e) {
                throw new ArchivoNoPudoSerAccedidoException();
            }
        }
    }

    //Método que verifica la conexión a una determinada página web
    private static void validateHttpRequest(URL url) throws IOException {

        HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
        //Esta linea fallará si no existe conexión con la URL
        Object objData = urlConnect.getContent();
    }

    //Método que pide al usuario un campo faltante y devuelve el valor
    private static String obtenerCampoFaltante(ConfKey claveFaltante) {
        System.out.print("El campo " + claveFaltante.name() + " no ha podido ser detectado en el archivo " + CONF_FILE_NAME + ". Por favor ingrese el valor del mismo: ");
        return (new Scanner(System.in).nextLine());
    }

    //Método que se encarga de cargar el driver correspondiente y abrirlo en la URL deseada
    private static WebDriver webDriverOpener(File logFile) {
        System.setProperty("webdriver.chrome.driver",chrome_driver);
        WebDriver driver;
        try {
            driver = new ChromeDriver();
        } catch (IllegalStateException e) {
            throw new DriverPathNoValidoException();
        }

        //Verifico si la URL fue parseada
        if (openair.equals(""))
            //Informo por pantalla el faltante de la URL y la pido manualmente
            openair = obtenerCampoFaltante(ConfKey.OPENAIR);
        try {
            validateHttpRequest(new URL(openair));
        } catch (MalformedURLException e) {
            closeDriver(driver, logFile);
            throw new URLMalConformadaException();
        } catch (IOException e) {
            closeDriver(driver, logFile);
            throw new NoInternetConnectionException();
        }

        logWriter(logFile, DRIVER_INIT);
        driver.get(openair);
        return driver;
    }

    //Método que verifica si los campos de Login han sido parseados del confFile (uso nuevamente reflection)
    private static void keysValidator() {
        for(ConfKey key : ConfKey.values()) {
            //Tener en cuenta que las variables asociadas a las claves tienen el mismo nombre pero toda en minúsculas
            Field variableAsociada = obtenerVariableDeNombre(key.name().toLowerCase());
            if (obtenerValorDeVariable(variableAsociada).equals("")) //El String se encuentra vacío
                modificarVariableString(variableAsociada, obtenerCampoFaltante(key));
        }
    }

    //Método que se encarga del Logueo en la página Web
    private static void login(WebDriver driver, WebDriverWait wait, File logFile) {
        //Detecto campos de Login y los vacío
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input_company")));
        WebElement companyField = driver.findElement(By.id("input_company"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input_user")));
        WebElement userField = driver.findElement(By.id("input_user"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input_password")));
        WebElement passwordField = driver.findElement(By.id("input_password"));
        companyField.clear();
        userField.clear();
        passwordField.clear();

        //Ingreso datos de company, user y password y logueo
        companyField.sendKeys(company);
        userField.sendKeys(user);
        passwordField.sendKeys(password);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("oa_comp_login_submit")));
        driver.findElement(By.id("oa_comp_login_submit")).click();

        //Valido que el Login haya sido exitoso a través de la URL
        if (!driver.getCurrentUrl().contains("login=1"))
            throw new LoginNoValidoException();
        logWriter(logFile, LOGIN_VALID);
    }
}


