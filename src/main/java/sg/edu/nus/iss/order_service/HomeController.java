package sg.edu.nus.iss.order_service;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public  class HomeController {

    Logger logger = org.slf4j.LoggerFactory.getLogger(HomeController.class);

    @RequestMapping("/")
    public String home() {
        logger.info("{\"message\": \"Welcome to Shopsmart Order Management"+"\"}");
        return "Welcome to Shopsmart Order Management";
    }


    @RequestMapping("/home")
    public String homeMethod() {
        logger.info("{\"message\": \"Welcome to Shopsmart Order Management Home"+"\"}");
        return "Welcome to Shopsmart Order Management Home";
    }
}
