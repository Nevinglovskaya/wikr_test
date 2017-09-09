import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Created by Corwin on 07.09.2017.
 */
public class CompareIphonesTest {
    WebDriver driver;
    Connection conn;
    Statement stmt;

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/";
    static final String USER = "root";
    static final String PASS = "root";

    String dbName = "PRODUCTS";
    String tableName = "PHONES";

    String iPhoneSeven = "Apple iPhone 7";
    String iPhoneSevenPlus = "Apple iPhone 7 Plus";
    String url = "https://rozetka.com.ua/";

    String iPhoneSevenWeight = "138";
    String iPhoneSevenPlusWeight = "188";
    String iPhoneSevenDiagonal = "4.7 \"";
    String iPhoneSevenPlusDiagonal = "5.5 \"";
    String iPhoneSevenRam = "2 ГБ";
    String iPhoneSevenPlusRam = "3 ГБ";

    @BeforeMethod
    public void setProperty() {
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files (x86)\\Google\\Chrome\\Application\\" +
                "chromedriver.exe");
    }

    @AfterMethod
    public void quit() throws SQLException {
        driver.quit();
        conn.close();
    }

    @Test
    public void testIphonesHaveSimilarCharakteristics() throws SQLException, ClassNotFoundException {
        driver = new ChromeDriver();

        // Go to the site
        driver.get(url);
        driver.manage().window().maximize();

        // Find iPhone7 characteristics
        WebElement product1 = searchForIphoneSeven(iPhoneSeven);
        product1.click();
        String iPhoneSevenPrice = getProductPrice();
        String iPhoneSevenFullName = getProductFullName();
        addProductToCart();
        Map<String, String> iPhoneSevenCharacteristics = getProductCharacteristics();

        // Find iPhone7 Plus characteristics
        WebElement product2 = searchForIphoneSevenPlus(iPhoneSevenPlus);
        product2.click();
        String iPhoneSevenPlusPrice = getProductPrice();
        String iPhoneSevenPlusFullName = getProductFullName();
        addProductToCart();
        Map<String, String> iPhoneSevenPlusCharacteristics = getProductCharacteristics();

        // Compare actual characteristics with stated
        Assert.assertEquals(iPhoneSevenCharacteristics.get("Вес, г"), iPhoneSevenWeight,
                "Weight of IPhone7 is different");
        Assert.assertEquals(iPhoneSevenPlusCharacteristics.get("Вес, г"), iPhoneSevenPlusWeight,
                "Weight of IPhone7 Plus is different");
        Assert.assertEquals(iPhoneSevenCharacteristics.get("Диагональ экрана"), iPhoneSevenDiagonal,
                "Diagonal of IPhone7 is different");
        Assert.assertEquals(iPhoneSevenPlusCharacteristics.get("Диагональ экрана"), iPhoneSevenPlusDiagonal,
                "Diagonal of IPhone7 Plus is different");
        Assert.assertEquals(iPhoneSevenCharacteristics.get("Оперативная память"), iPhoneSevenRam,
                "RAM of IPhone7 is different");
        Assert.assertEquals(iPhoneSevenPlusCharacteristics.get("Оперативная память"), iPhoneSevenPlusRam,
                "RAM of IPhone7 Plus is different");

        // Compare characteristics of both iPhones
        ArrayList<String> equalParameters = new ArrayList<String>();
        for (String key : iPhoneSevenCharacteristics.keySet()) {
            if (iPhoneSevenCharacteristics.get(key).equals(iPhoneSevenPlusCharacteristics.get(key))) {
                equalParameters.add(key);
            }
        }
        System.out.println("Both phones have such equal characteristics: \n");
        for (String param : equalParameters) {
            System.out.println(param + "\n");
        }

        // Check if phones are present in the cart
        driver.findElement(By.xpath("//span[contains(text(), 'Корзина')]")).click();
        ArrayList<String> productNamesInCart = getProductNamesInCart();
        Assert.assertEquals(productNamesInCart.size(), 2,
                "Number of products in the cart is incorrect");
        Assert.assertTrue(productNamesInCart.contains(iPhoneSevenFullName),
                "iPhone7 wasn't added to the cart");
        Assert.assertTrue(productNamesInCart.contains(iPhoneSevenPlusFullName),
                "iPhone7 Plus wasn't added to the cart");

        // Create database and insert date, names and prices of the products there
        setConnection();
        createDataBase(dbName);
        createDataBaseTable(dbName, tableName);
        insertValuesIntoDataBase(iPhoneSeven, iPhoneSevenPrice);
        insertValuesIntoDataBase(iPhoneSevenPlus, iPhoneSevenPlusPrice);
        System.out.println("Values are added to the database");
    }

    public WebElement searchForIphoneSeven(String productName) {
        List<WebElement> productList = searchForProduct(productName);
        WebElement product = null;
        for (WebElement item : productList) {
            String productTitle = item.getText();
            if (productTitle.contains(productName) && !productTitle.contains("Plus")) {
                product = item;
                break;
            }
        }
        return product;
    }

    public WebElement searchForIphoneSevenPlus(String productName) {
        List<WebElement> productList = searchForProduct(productName);
        WebElement product = null;
        for (WebElement item : productList) {
            String productTitle = item.getText();
            if (productTitle.contains(productName)) {
                product = item;
                break;
            }
        }
        return product;
    }

    public List<WebElement> searchForProduct(String productName) {
        WebElement searchInput = driver.findElement(By.xpath("//input[@placeholder='Поиск']"));
        WebElement searchButton = driver.findElement(By.xpath("//button[@name='rz-search-button']"));
        searchInput.clear();
        searchInput.sendKeys(productName);
        searchButton.click();
        List<WebElement> productList = driver.findElements(By.xpath("//div[@class='g-i-tile-i-title clearfix']"));
        return productList;
    }

    public Map<String, String> getProductCharacteristics() {
        Map<String, String> characteristics = new HashMap<String, String>();
        WebElement characteristicTab = driver.findElement(By.xpath("//li[@name='characteristics']"));
        String className = "pp-characteristics-tab-i";
        characteristicTab.click();
        new WebDriverWait(driver, 5)
                .until(ExpectedConditions.visibilityOfElementLocated(By.className(className)));
        List<WebElement> listOfCharacteristics = driver.findElements(By.className(className));
        for (WebElement element : listOfCharacteristics) {
            String title = element.findElement(By.className("pp-characteristics-tab-i-title")).getText().trim();
            String field = element.findElement(By.className("pp-characteristics-tab-i-field")).getText().trim();
            characteristics.put(title, field);
        }
        return characteristics;
    }

    public String getProductPrice() {
        WebElement price = driver.findElement(By.id("price_label"));
        return price.getText().trim();
    }

    public String getProductFullName() {
        WebElement title = driver.findElement(By.xpath("//h1[@class='detail-title']"));
        return title.getText().trim();
    }

    public ArrayList<String> getProductNamesInCart() {
        ArrayList<String> productNamesInCart = new ArrayList<String>();
        new WebDriverWait(driver, 5)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//div[@class='cart-i-title']")));
        List<WebElement> productListInCart = driver.findElements(By.xpath("//div[@class='cart-i-title']"));
        for (WebElement element : productListInCart) {
            String name = element.getText().trim();
            productNamesInCart.add(name);
        }
        return productNamesInCart;
    }

    public void addProductToCart() {
        driver.findElement(By.xpath("//button[contains(text(), 'Купить')]")).click();
        new WebDriverWait(driver, 5)
                .until(ExpectedConditions.presenceOfElementLocated((By.xpath("//div[@class='cart-g-kit-slider-inner']"))));
        driver.navigate().refresh();
    }

    public void setConnection() throws ClassNotFoundException, SQLException {
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        stmt = conn.createStatement();
    }

    public void createDataBase(String dbName) throws SQLException {
        stmt.executeUpdate("CREATE DATABASE " + dbName);
        System.out.println("Database is created");
    }

    public void createDataBaseTable(String dbName, String tableName) throws SQLException {
        stmt.execute("USE " + dbName);
        stmt.execute("CREATE TABLE " + tableName + "(" +
                "TESTDATE DATE, " +
                "NAME VARCHAR (20), " +
                "PRICE VARCHAR (10))");
    }

    public void insertValuesIntoDataBase(String name, String price) throws SQLException {
        Date sqlDate = new Date(Calendar.getInstance().getTime().getTime());
        stmt.execute("INSERT INTO " + tableName + " VALUES ('" + sqlDate + "', '" + name + "', '" + price + "')");
    }
}
