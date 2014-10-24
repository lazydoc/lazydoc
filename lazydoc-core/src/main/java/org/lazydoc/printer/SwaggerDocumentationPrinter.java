package org.lazydoc.printer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.lazydoc.model.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.join;

public class SwaggerDocumentationPrinter extends DocumentationPrinter {

	@Override
	public void print(Map<Integer, DocDomain> domains, Map<String, DocDataType> dataTypes, String target, Set<DocError> listOfCommonErrors)
			throws Exception {
		this.domains = domains;
		this.dataTypes = dataTypes;
		this.listOfCommonErrors = listOfCommonErrors;
		printSwaggerToJsp(target);
	}

	private void printSwaggerToJsp(String target) throws Exception {
		addDomainsAndDataTypesToFilelist();
		createDiscover();
		createAndWriteViewControllerXml(target);
		writeFiles(FilenameUtils.normalizeNoEndSeparator(target) + "/views/");
	}

	private void addDomainsAndDataTypesToFilelist() {
		for (DocDomain domain : domains.values()) {
			if (domain.getSubDomains().isEmpty()) {
				files.put("api/" + domain.getDomain().toLowerCase() + ".jsp", printDomainToJson(domain));
			} else {
				for (DocSubDomain subDomain : domain.getSubDomains().values()) {
					files.put("api/" + (domain.getDomain() + "-" + subDomain.getSubDomain()).toLowerCase() + ".jsp",
							printDomainToJson(subDomain));
				}
			}
		}
		for (DocDataType dataType : dataTypes.values()) {
			files.put(getDataTypeFilename(dataType.getName()), printDataTypeToJson(dataType));
		}
	}

	public String printDomainToJson(DocDomain domain) {
		return printDomainOrSubDomainToJson(domain.getOperations(), domain.getDomain(), domain.getBasePath());
	}

	public String printSubDomainToJson(Map<String, String> files, DocSubDomain subDomain) {
		return printDomainOrSubDomainToJson(subDomain.getOperations(), subDomain.getDomain() + "-" + subDomain.getSubDomain(),
				subDomain.getBasePath());
	}

	public String printDomainOrSubDomainToJson(Set<DocOperation> docOperations, String domainName, String basePath) {
		String json = "<%@ page contentType=\"application/json; charset=UTF-8\" %>\n";
		json += "{\n";
		json += "   \"basePath\": \"" + basePath + "\",\n";
		json += "   \"apis\": [\n";
		List<String> apis = new ArrayList<String>();
		for (DocOperation operation : docOperations) {
			apis.add(printApiOperationToJson(domainName, operation));
		}
		json += join(apis, ",\n");
		json += "   ]\n";
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
		json += "   \"httpMethod\": \"" + operation.getHttpMethod() + "\",\n";
		json += "   \"nickname\": \"" + operation.getNickname() + "\",\n";
		json += "   \"responseClass\": \"" + operation.getResponseClass() + "\",\n";
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
		json += "   \"dataType\": \"" + parameter.getDataType() + "\",\n";
		json += "   \"required\": " + parameter.isRequired() + ",\n";
		json += "   \"allowMultiple\": " + parameter.isAllowMultiple() + ",\n";
		json += "   \"list\": " + parameter.isList() + "\n";
		json += "}\n";
		return json;
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
			json += "              \"enum\": [\"" + property.getEnumValues().replaceAll(", ", "\", \"") + "\"],\n";
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

	private void createAndWriteViewControllerXml(String target) throws IOException {
		List<String> mvcViews = new ArrayList<String>();
		mvcViews.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		mvcViews.add("<beans xmlns=\"http://www.springframework.org/schema/beans\"");
		mvcViews.add("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		mvcViews.add("xmlns:context=\"http://www.springframework.org/schema/context\"");
		mvcViews.add("xmlns:mvc=\"http://www.springframework.org/schema/mvc\"");
		mvcViews.add("xsi:schemaLocation=\"http://www.springframework.org/schema/mvc");
		mvcViews.add("http://www.springframework.org/schema/mvc/spring-mvc-3.1.xsd http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.1.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.1.xsd\">");
		for (DocDomain domain : domains.values()) {
			if (!domain.getOperations().isEmpty()) {
				mvcViews.add("<mvc:view-controller path=\"/discover/" + StringUtils.capitalize(domain.getDomain()) + "\" view-name=\"/api/"
						+ domain.getDomain().toLowerCase() + "\" />");
			}
			for (DocSubDomain subDomain : domain.getSubDomains().values()) {
				mvcViews.add("<mvc:view-controller path=\"/discover/"
						+ StringUtils.capitalize(domain.getDomain() + "-" + subDomain.getSubDomain()) + "\" view-name=\"/api/"
						+ (domain.getDomain() + "-" + subDomain.getSubDomain()).toLowerCase() + "\" />");
			}
		}
		mvcViews.add("</beans>");
		File viewControllers = new File(target + "/spring/view-controller.xml");
		viewControllers.getParentFile().mkdirs();
		FileUtils.writeLines(viewControllers, mvcViews);
	}

	private void createDiscover() {
		List<String> apis = new ArrayList<String>();
		for (DocDomain domain : domains.values()) {
			if (!domain.getOperations().isEmpty()) {
				apis.add(createApiPathJson(domain));
			}
			for (DocSubDomain subDomain : domain.getSubDomains().values()) {
				apis.add(createApiPathJson(subDomain));
			}
		}
		List<String> models = new ArrayList<String>();
		for (String name : dataTypes.keySet()) {
			models.add("           <%@include file=\"" + getDataTypeFilename(name) + "\" %>");
		}

		String json = "<%@ page contentType=\"application/json; charset=UTF-8\" %>\n";
		json += "{\n";
		json += "        <%@include file=\"includes/path.jsp\" %>\n";
		json += "       \"apis\":[\n";
		json += join(apis, ",\n") + "\n";
		json += "       ],\n";
		json += "       \"models\": {\n";
		json += join(models, ",\n") + "\n";
		json += "       }\n";
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

	private String getDataTypeFilename(String name) {
		return "api/_types/" + name.toLowerCase() + ".jsp";
	}

}
