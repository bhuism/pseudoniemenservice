package nl.appsource.controller.v1;

import org.springframework.web.bind.annotation.RequestMapping;


@RequestMapping(VersionOneController.V1)
@SuppressWarnings("InterfaceIsType")
public interface VersionOneController {

    @SuppressWarnings({"checkstyle:InterfaceIsType", "checkstyle:interfaceistype"})
    String V1 = "/api/v1";

}
