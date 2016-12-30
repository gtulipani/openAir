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
        COMPANY,
        USER,
        PASSWORD,
        CHARGE_CODE,
        DATE,
        CHROME_DRIVER;
    }

    //confFile parameters
    private static String company;
    private static String user;
    private static String password;
    private static String charge_code;
    private static String date;
    private static String chrome_driver;

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

    private final static String OPEN_AIR_URL = "https://www.openair.com/index.pl";
    private final static String OPEN_AIR_X053_COMPANY_PROJECT = "AT&T : (X053) Software Engineering Expense";
    private final static String OPEN_AIR_TIMESHEET_CREADA = "Se ha creado la Timesheet correspondiente a la fecha " + date + ".";
    private final static String OPEN_AIR_TIMESHEET_SUBMITTEADA = "Se ha submitteado la Timesheet correspondiente a la fecha " + date + " con el " + ConfKey.CHARGE_CODE.name() + "= \"" + charge_code + "\"";
    private final static String OPEN_AIR_SCREENSHOT_NO_CREADA = "No se ha podido crear la screenshot correspondiente para el día " + date + ".";
    private final static String OPEN_AIR_SCREENSHOT_CREADA = "Se ha creado la screenshot correspondiente para el día " + date + ".";

    //webDriver related constants
    private final static String DRIVER_INIT = "El driver del navegador ha sido cargado.";
    private final static String DRIVER_CLOSING = "El driver del navegador ha sido cerrado.";
    private final static String DRIVER_PATH_NOT_VALID = "El driver del navegador no puso ser cargado. Revise el valor de " + ConfKey.CHROME_DRIVER.name() + ".";
    private final static String URL_NOT_VALID = "La URL especificada para la aplicación: \"" + OPEN_AIR_URL + "\" se encuentra mal conformada. Por favor revísela.";
    private final static String URL_NOT_REACHABLE = "La URL especificada para la aplicación: \"" + OPEN_AIR_URL + "\" no se puede cargar. Revise la misma y la conexión a internet.";
    private final static String LOGIN_NOT_VALID = "El logueo en la aplicación no ha sido exitoso. Revise los valores de " + ConfKey.COMPANY.name() + ", " + ConfKey.USER.name() + " y " + ConfKey.PASSWORD.name() + ".";
    private final static String LOGIN_VALID = "El logueo en la aplicación ha sido exitoso.";

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

    private static void analizeConfFile(File logFile) {
        try {
            parseConfigurationFile(logFile);
        } catch (RutaDeArchivoNoValidaException e) {
            logWriter(logFile, CONF_FILE_NOT_FOUND);
            System.exit(1);
        }
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
    private static void esperarConfirmaciónDelUsuario() {
        System.out.print("Ha ocurrido un error al momento de sacar la screenshot. Puede sacarla ahora y oprimir una tecla una vez haya finalizado...");
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
        dateSelector.selectByVisibleText(date);

        //Oprimo el botón Save
        driver.findElement(By.xpath("//*[@id=\"formButtonsBottom\"]/input[2]")).click();
        logWriter(logFile, OPEN_AIR_TIMESHEET_CREADA);

        //Completo el campo correspondiente a Company: Project
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ts_c2_r1")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ts_c2_r1")));
        Select companySelector = new Select(driver.findElement(By.id("ts_c2_r1")));
        companySelector.selectByVisibleText(OPEN_AIR_X053_COMPANY_PROJECT);

        //Guardo todas las Task correspondientes al Project y elijo aquella que empieza con el CHARGE_CODE que obtuve
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ts_c3_r1")));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ts_c3_r1")));
        Select taskSelector = new Select(driver.findElement(By.id("ts_c3_r1")));
        String taskCompleteName = obtenerNombreChargeCode(driver);
        taskSelector.selectByVisibleText(taskCompleteName);

        //Completo los campos correspondientes con 8 horas
        driver.findElement(By.id("ts_c6_r1")).sendKeys("8");
        driver.findElement(By.id("ts_c7_r1")).sendKeys("8");
        driver.findElement(By.id("ts_c8_r1")).sendKeys("8");
        driver.findElement(By.id("ts_c9_r1")).sendKeys("8");
        driver.findElement(By.id("ts_c10_r1")).sendKeys("8");

        //Oprimo Save and Submit
        driver.findElement(By.id("save_grid_submit")).click();
        logWriter(logFile, OPEN_AIR_TIMESHEET_SUBMITTEADA);

        //Saco captura de pantalla para enviar por mail
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, new File(obtenerNombreScreenshot()));
        } catch (IOException | NoSePudoGenerarScreenshotException e) {
            logWriter(logFile, OPEN_AIR_SCREENSHOT_NO_CREADA);
            esperarConfirmaciónDelUsuario();
            closeDriver(driver, logFile);
            System.exit(1);
        }

        logWriter(logFile, OPEN_AIR_SCREENSHOT_CREADA);

        //Cierro el navegador
        closeDriver(driver, logFile);
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

    //Método que se encarga de analizar línea del confFile y sobreescribir las variables estáticas mediante el uso de Reflection
    private static void parseLine(String linea) {
        String claveLeida = parseKey(linea);

        //Comienzo del uso de Reflection para modificar la variable correspondiente a la clave
        {
            Field field;
            Class<?> c = (new Main()).getClass();

            try {
                field = c.getDeclaredField(claveLeida.toLowerCase());
            } catch (NoSuchFieldException e) {
                //Si la clave leida no tiene su correspondiente variable
                throw new ClaveNoValidaException();
            }

            field.setAccessible(true);

            try {
                field.set(c, linea.substring(ConfKey.valueOf(claveLeida).name().length() + 1));
            } catch (IllegalAccessException e) {
                throw new NoPermissionOnJVMReflectionException();
            }

            field.setAccessible(false);
        }
        //Fin del uso de Reflection para modificar la variable correspondiente a la clave
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

    //Método que se encarga de cargar el driver correspondiente y abrirlo en la URL deseada
    private static WebDriver webDriverOpener(File logFile) {
        System.setProperty("webdriver.chrome.driver",chrome_driver);
        WebDriver driver;
        try {
            driver = new ChromeDriver();
        } catch (IllegalStateException e) {
            throw new DriverPathNoValidoException();
        }
        try {
            validateHttpRequest(new URL(OPEN_AIR_URL));
        } catch (MalformedURLException e) {
            closeDriver(driver, logFile);
            throw new URLMalConformadaException();
        } catch (IOException e) {
            closeDriver(driver, logFile);
            throw new NoInternetConnectionException();
        }

        logWriter(logFile, DRIVER_INIT);
        driver.get(OPEN_AIR_URL);
        return driver;
    }

    private static void userCredentialsValidator() {
        if (company.equals("")) {
            System.out.print("El campo " + ConfKey.COMPANY.name() + " no ha podido ser detectado en el archivo " + CONF_FILE_NAME + ". Por favor ingrese el valor del mismo: ");
            company = (new Scanner(System.in).nextLine());
        }
        if (user.equals("")) {
            System.out.print("El campo " + ConfKey.USER.name() + " no ha podido ser detectado en el archivo " + CONF_FILE_NAME + ". Por favor ingrese el valor del mismo: ");
            user = (new Scanner(System.in).nextLine());
        }
        if (password.equals("")) {
            System.out.print("El campo " + ConfKey.PASSWORD.name() + " no ha podido ser detectado en el archivo " + CONF_FILE_NAME + ". Por favor ingrese el valor del mismo: ");
            password = (new Scanner(System.in).nextLine());
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

        //Valido que existan los campos de company, user y password
        //userCredentialsValidator();

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


