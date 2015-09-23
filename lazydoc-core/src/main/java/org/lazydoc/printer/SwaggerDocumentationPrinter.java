package org.lazydoc.printer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.lazydoc.config.PrinterConfig;
import org.lazydoc.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.join;

public class SwaggerDocumentationPrinter extends DocumentationPrinter {

	// TODO update SwaggerDocumentationPrinter

    private Set<String> models = new HashSet<>();

    @Override
    public void print(PrinterConfig printerConfig) throws Exception {
        this.printerConfig = printerConfig;
        printSwaggerToJsp();
    }

    private String getDataTypeFilename(String name) {
        return "api/_types/" + name.toLowerCase() + ".jsp";
    }

	private void printSwaggerToJsp() throws Exception {
		addDomainsAndDataTypesToFilelist();
		createDiscover();
        createAndWriteViewController();
		writeFiles(FilenameUtils.normalize(printerConfig.getOutputPath()));
	}

	private void addDomainsAndDataTypesToFilelist() {
		for (DocDomain domain : printerConfig.getDomains().values()) {
			if (domain.getSubDomains().isEmpty()) {
				files.put("api/" + domain.getDomain().toLowerCase() + ".jsp", printDomainToJson(domain));
			} else {
				for (DocSubDomain subDomain : domain.getSubDomains().values()) {
					files.put("api/" + (domain.getDomain() + "-" + subDomain.getSubDomain()).toLowerCase() + ".jsp",
							printDomainToJson(subDomain));
				}
			}
		}
		for (DocDataType dataType : printerConfig.getDataTypes().values()) {
			files.put(getDataTypeFilename(dataType.getName()), printDataTypeToJson(dataType));
		}
	}

	public String printDomainToJson(DocDomain domain) {
		return printDomainOrSubDomainToJson(domain.getOperations(), domain.getDomain());
	}

	public String printSubDomainToJson(Map<String, String> files, DocSubDomain subDomain) {
		return printDomainOrSubDomainToJson(subDomain.getOperations(), subDomain.getDomain() + "-" + subDomain.getSubDomain());
	}

	public String printDomainOrSubDomainToJson(Set<DocOperation> docOperations, String domainName) {
        models.clear();
        String basePath = printerConfig.getParams().get("swagger.basepath");
        if(StringUtils.isBlank(basePath)) {
            throw new RuntimeException("Please provide the swagger.basepath in the printer config params");
        }

		String json = "<%@ page contentType=\"application/json; charset=UTF-8\" %>\n";
		json += "{\n";
        json += "   \"basePath\": \"" + basePath + "\",\n";
		json += "   \"apis\": [\n";
		List<String> apis = new ArrayList<String>();
        for (DocOperation operation : docOperations) {
            apis.add(printApiOperationToJson(domainName, operation));
        }
        json += join(apis, ",\n");
        json += "   ],\n";
        json += "   \"models\": {\n";
        json += StringUtils.join(models, ",");
        json += "   }\n";
        json += "}\n";

		return json;
	}

	public String printApiOperationToJson(String domainName, DocOperation operation) {
		operation.setFileName("api/_ops/" + domainName + "/" + operation.getNickname() + ".jsp");
		String json = "       {\n";
		json += "           \"path\": \"" + operation.getPath() + "\",\n";
		json += "           \"description\": \"" + operation.getShortDescription() + "\",\n";
		json += "           \"operations\": [\n";
		json += "                <%@include file=\"" + operation.getFileName().replaceAll("api/", "") + "\" %>";
		json += "           ]\n";
		json += "       }";
		files.put(operation.getFileName(), printOperationToJson(operation));
		return json;
	}

	public String printOperationToJson(DocOperation operation) {
		List<String> parameters = new ArrayList<String>();
		String json = "{\n";
		json += "   \"method\": \"" + operation.getHttpMethod() + "\",\n";
		json += "   \"nickname\": \"" + operation.getNickname() + "\",\n";
		json += "   \"responseClass\": \"" + getOperationResponse(operation.getOperationResponse()) + "\",\n";
		json += "   \"parameters\": [\n";
		for (DocParameter parameter : operation.getParameters()) {
			parameters.add("       <%@include file=\"../../_params/" + parameter.getReferenceName() + ".jsp\" %>");
			files.put("api/_params/" + parameter.getReferenceName() + ".jsp", printParameterToJson(parameter));
		}
		json += join(parameters, ",\n") + "\n";
		json += "   ],\n";
		json += "   \"summary\": \"" + operation.getShortDescription() + "\",\n";
		json += "   \"notes\": \"" + operation.getNotes() + "\",\n";
		json += "   \"authorization\": \"\",\n";
		json += "   \"errorResponses\": [],\n";
		json += "   \"level\": \"\"\n";
		json += "}\n";
		return json;
	}

	private String getOperationResponse(DocOperationResponse operationResponse) {
		if(operationResponse.isInList()) {
			return "List["+operationResponse.getResponseType()+"]";
		}
		return operationResponse.getResponseType();
	}

	public String printParameterToJson(DocParameter parameter) {
		String json = "{\n";
		String paramType = parameter.getParamType();
		json += "   \"paramType\": \"" + paramType + "\",\n";
		if (paramType.equals("body")) {
			json += "   \"name\": \"requestBody\",\n";
		} else {
			json += "   \"name\": \"" + parameter.getName() + "\",\n";
		}
		json += "   \"description\": \"" + parameter.getDescription() + "\",\n";
		json += "   \"type\": \"" + parameter.getDataType() + "\",\n";
		json += "   \"required\": " + parameter.isRequired() + ",\n";
		json += "   \"allowMultiple\": " + parameter.isAllowMultiple() + ",\n";
		json += "   \"list\": " + parameter.isList() + "\n";
		json += "}\n";

        addModelOfParameter(parameter.getDataType());

        return json;
	}

    private void addModelOfParameter(String dataType) {
        DocDataType docDataType = printerConfig.getDataTypes().get(dataType);
        if (docDataType != null) {
            models.add("           <%@include file=\"" + getDataTypeFilename(dataType).replaceAll("api/", "") + "\" %>");
            for(DocProperty property : docDataType.getProperties()) {
                addModelOfParameter(property.getType());
            }
        }
    }

    public String printDataTypeToJson(DocDataType dataType) {
		List<String> properties = new ArrayList<String>();
		String json = "\"" + dataType.getName() + "\": {\n";
		json += "   \"properties\": {\n";
		for (DocProperty property : dataType.getProperties()) {
			properties.add(printPropertyToJson(property));
		}
		json += join(properties, ",\n") + "\n";
		json += "   }\n";
		json += "}\n";
		return json;
	}

	public String printPropertyToJson(DocProperty property) {
		String json = "         \"" + property.getName() + "\": {\n";
		if (property.isList()) {
			json += "              \"type\": \"List[" + property.getType() + "]\",\n";
		} else {
			json += "              \"type\": \"" + property.getType() + "\",\n";
		}
		if (property.hasEnumValues()) {
			json += "              \"enum\": [\"" + property.getEnumValues() + "\"],\n";
		}
		json += "              \"description\": \"" + property.getDescription() + "\",\n";
		if (property.hasSample()) {
			json += "              \"sample\": " + property.getSampleString() + ",\n";
		}
		json += "              \"required\": " + property.isRequired() + ",\n";
		json += "              \"request\": " + property.isRequest() + ",\n";
		json += "              \"response\": " + property.isResponse() + ",\n";
		json += "              \"list\": " + property.isList() + "\n";
		json += "           }";
		return json;
	}

	private void createAndWriteViewController() throws IOException {
        String viewPropertiesFilename = printerConfig.getParams().get("swagger.views.properties.filename");
        String swaggerDirectoryName = printerConfig.getParams().get("swagger.directory.name");

        if(StringUtils.isBlank(viewPropertiesFilename)) {
            throw new RuntimeException("Please provide the swagger.views.properties.filename in printer config params");
        }
        if(StringUtils.isBlank(swaggerDirectoryName)) {
            throw new RuntimeException("Please provide the swagger.directory.name in printer config params");
        }

		List<String> mvcViews = new ArrayList<String>();
		for (DocDomain domain : printerConfig.getDomains().values()) {
			if (!domain.getOperations().isEmpty()) {
                addViewMapping(mvcViews, domain.getDomain(), swaggerDirectoryName);
            }
			for (DocSubDomain subDomain : domain.getSubDomains().values()) {
                addViewMapping(mvcViews, domain.getDomain()+"-"+subDomain.getSubDomain(), swaggerDirectoryName);
            }
		}

        File viewControllers = new File(viewPropertiesFilename);
		viewControllers.getParentFile().mkdirs();
		FileUtils.writeLines(viewControllers, mvcViews);
	}

    private void addViewMapping(List<String> mvcViews, String domain, String swaggerDirectoryName) {
        mvcViews.add("/discover/" + StringUtils.capitalize(domain) + "=/" + swaggerDirectoryName + "/" + domain.toLowerCase());
    }

    private void createDiscover() {
		List<String> apis = new ArrayList<String>();
		for (DocDomain domain : printerConfig.getDomains().values()) {
			if (!domain.getOperations().isEmpty()) {
				apis.add(createApiPathJson(domain));
			}
			for (DocSubDomain subDomain : domain.getSubDomains().values()) {
				apis.add(createApiPathJson(subDomain));
			}
		}

		String json = "<%@ page contentType=\"application/json; charset=UTF-8\" %>\n";
		json += "{\n";
		json += "       \"apis\":[\n";
		json += join(apis, ",\n") + "\n";
		json += "       ]\n";
		json += "}\n";
		files.put("discover.jsp", json);
	}

	private String createApiPathJson(DocDomain domain) {
		String api = "          {\n";
		api += "                \"path\": \"/" + StringUtils.capitalize(domain.getDomain()) + "\",\n";
		api += "                \"description\": \"" + domain.getDescription() + "\"\n";
		api += "          }";
		return api;
	}

}
