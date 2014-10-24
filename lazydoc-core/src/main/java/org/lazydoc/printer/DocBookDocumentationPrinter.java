package org.lazydoc.printer;

import com.cedarsoftware.util.io.JsonWriter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.lazydoc.annotation.InsertPosition;
import org.lazydoc.model.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class DocBookDocumentationPrinter extends DocumentationPrinter {

	@Override
	public void print(Map<Integer, DocDomain> domains, Map<String, DocDataType> dataTypes, String target, Set<DocError> listOfCommonErrors)
			throws Exception {
		this.domains = domains;
		this.dataTypes = dataTypes;
		this.listOfCommonErrors = listOfCommonErrors;
		printDocBookXML(target);
	}

	private void printDocBookXML(String target) throws Exception {
		createApiDocXML();
		createChapters();
		writeFiles(FilenameUtils.normalizeNoEndSeparator(target));
	}

	private void createApiDocXML() {
		String xml = printVersion();
		xml += printStartTagWithAttributes(
				"book",
				"xmlns=\"http://docbook.org/ns/docbook\" xml:lang=\"en\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:svg=\"http://www.w3.org/2000/svg\" xmlns:m=\"http://www.w3.org/1998/Math/MathML\" xmlns:html=\"http://www.w3.org/1999/xhtml\" version=\"5.0\"");
		xml += "<?dbhtml dir=\"apidoc-gen\" ?>";
		xml += printStartTag("bookinfo");
		xml += printFullTag("title", "Internet Business Suite");
		xml += printFullTag("subtitle", "API documentation");
		xml += printFullTag("corpauthor", "Deutsche Telekom AG - Products &amp; Innovation");

		xml += printStartTag("copyright");
		for (int year = 2011; year <= getYearFromDate(); year++) {
			xml += printFullTag("year", "" + year);
		}
		xml += printFullTag("holder", "Deutsche Telekom AG");
		xml += printEndTag("copyright");

		xml += printStartTag("legalnotice");
		xml += printFullTag("para", "All data is subject to copyright. Reproduction is prohibited.");
		xml += printEndTag("legalnotice");

		xml += printFullTag("edition", "<?eval ${project.version}?>");

		xml += printStartTag("keywordset");
		xml += printFullTag("keyword", "GCP");
		xml += printFullTag("keyword", "API");
		xml += printEndTag("keywordset");

		xml += printEndTag("bookinfo");

		xml += printShortTag("toc");

		xml += printShortTagWithAttributes("xi:include", "href=\"preface.xml\"");
		xml += printShortTagWithAttributes("xi:include", "href=\"static/introduction.xml\"");
		for (DocDomain domain : domains.values()) {
			xml += printShortTagWithAttributes("xi:include", "href=\"chapters/" + domain.getDomain().toLowerCase() + ".xml\"");
		}
		xml += printCommonErrorDescriptions();
		xml += printShortTagWithAttributes("xi:include", "href=\"../_common/biblio.xml\"");
		xml += printShortTagWithAttributes("xi:include", "href=\"../_common/glossary.xml\"");

		xml += printEndTag("book");
		files.put("/apidoc-gen.xml", prettyFormat(xml, 4));
	}

	private void createChapters() {

		for (DocDomain domain : domains.values()) {
			String xml = printVersion();
			xml += printStartTagWithAttributes(
					"chapter",
					"version=\"5.0\" xml:lang=\"en\" xmlns=\"http://docbook.org/ns/docbook\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xml:id=\""
							+ domain.getDomain() + "_sect\"");
			xml += printFullTag("title", domain.getDomainShortDescription());
			xml += printFullTag("para", domain.getDescription());

			if (domain.hasExternalDocumentation() && domain.getExternalInsertPosition().equals(InsertPosition.TOP)) {
				xml += printShortTagWithAttributes("xi:include", "href=\"../static/" + domain.getExternalDocumentation() + ".xml\"");
			}

			if (!domain.getSubDomains().isEmpty()) {
				for (DocSubDomain subDomain : domain.getSubDomains().values()) {
					xml += printStartTag("sect1");
					xml += printFullTag("title", subDomain.getSubDomainShortDescription());
					xml += printFullTag("para", subDomain.getDescription());
					if (subDomain.hasExternalDocumentation()) {
						xml += printShortTagWithAttributes("xi:include", "href=\"../static/" + subDomain.getExternalDocumentation()
								+ ".xml\"");
					}
					xml += printApiOperationsForDomain(subDomain);
					xml += printCommonErrorList(subDomain.getErrorList(), "sect2");
					xml += printEndTag("sect1");

				}
			} else {
				xml += printApiOperationsForDomain(domain);
			}

			xml += printCommonErrorList(domain.getErrorList(), "sect2");

			if (domain.hasExternalDocumentation() && domain.getExternalInsertPosition().equals(InsertPosition.BOTTOM)) {
				xml += printShortTagWithAttributes("xi:include", "href=\"../static/" + domain.getExternalDocumentation() + ".xml\"");
			}

			xml += printEndTag("chapter");
			files.put("/chapters/" + domain.getDomain().toLowerCase() + ".xml", prettyFormat(xml, 4));
		}
	}

	private String printCommonErrorDescriptions() {
		String xml = printStartTag("chapter");
		xml += printFullTag("title", "List of comon error codes");
		xml += printFullTag("para",
				"This area contains a list of all errors which can occure through processing which is not specific to a certain topic.");
		xml += printCommonErrorList(listOfCommonErrors, "sect1");
		xml += printEndTag("chapter");
		return xml;
	}

	private String printApiOperationsForDomain(DocDomain domain) {
		String xiIncludes = "";

		String xml = printTableTop("Available REST methods", "1*,3*,3*", "HTTP method", "Context path", "Description");

		for (DocOperation operation : domain.getOperations()) {
			xml += printStartTag("row");
			xml += printFullTag("entry", operation.getHttpMethod());
			xml += printStartTag("entry");
			xml += printStartTagWithAttributes("link", "linkend=\"" + operation.getNickname().toLowerCase() + "\"");
			xml += printFullTag("uri", operation.getPath());
			xml += printEndTag("link");
			xml += printEndTag("entry");
			xml += printFullTag("entry", operation.getSummary());
			xml += printEndTag("row");
			xiIncludes += printShortTagWithAttributes("xi:include", "href=\"operations/" + domain.getDomain().toLowerCase() + "/"
					+ operation.getNickname().toLowerCase() + ".xml\"");
			createOperation(operation.getPath(), domain.getDomain(), operation);
		}

		xml += printTableBottom();
		xml += xiIncludes;
		return xml;
	}

	private void createOperation(String apiPath, String domain, DocOperation operation) {
		String xml = printVersion();
		xml += printStartTagWithAttributes("sect2",
				"xmlns=\"http://docbook.org/ns/docbook\" xml:lang=\"en\" xmlns:xi=\"http://www.w3.org/2001/XInclude\"	version=\"5.0\" xml:id=\""
						+ operation.getNickname().toLowerCase() + "\"");
		xml += printFullTag("title", operation.getShortDescription());
		xml += printFullTag("para", operation.getNotes());

		if (operation.hasExternalDocumentation() && operation.getExternalInsertPosition().equals(InsertPosition.TOP)) {
			xml += printShortTagWithAttributes("xi:include", "href=\"../../../static/" + operation.getExternalDocumentation() + ".xml\"");
		}

		xml += printStartTag("simplesect");
		xml += printFullTag("title", "Structure of the request");
		xml += printFullTag("para", "Schematic representation of the URI with its parameters:");
		xml += printFullTag("programlisting", operation.getHttpMethod() + " " + apiPath);
		xml += printFullTag("para", "The following parameters are expected:");

		xml += printTableTop("Description of the call parameters", "1*,1*,3*", "Parameter", "Type", "Description");
		for (DocParameter parameter : operation.getParameters()) {
			if (!parameter.getParamType().equals("body")) {
				xml += printStartTag("row");
				xml += printFullTag("entry", parameter.getName());
				xml += printFullTag("entry", parameter.getDataType());
				xml += printFullTag("entry", parameter.getDescription());
				xml += printEndTag("row");
			}
		}
		xml += printTableBottom();

		xml += printEndTag("simplesect");
		xml += printRequestBody(operation.getParameters());
		xml += printResponse(operation.getResponseClass());

		if (operation.hasExternalDocumentation() && operation.getExternalInsertPosition().equals(InsertPosition.BOTTOM)) {
			xml += printShortTagWithAttributes("xi:include", "href=\"../../../static/" + operation.getExternalDocumentation() + ".xml\"");
		}

		xml += printEndTag("sect2");
		files.put("/chapters/operations/" + domain.toLowerCase() + "/" + operation.getNickname().toLowerCase() + ".xml",
				prettyFormat(xml.replaceAll("&", "&amp;"), 4));
	}

	private String printRequestBody(List<DocParameter> parameters) {
		DocDataType dataType = null;
		DocParameter param = null;
		String xml = printStartTag("simplesect");
		for (DocParameter parameter : parameters) {
			if (parameter.getParamType().equals("body")) {
				dataType = dataTypes.get(parameter.getDataType());
				param = parameter;
				break;
			}
		}
		if (dataType != null) {
			xml += printTableTop("Structure of the request body", "2*,1*,3*", "Type", "Cardinality", "Description");
			xml += printStartTag("row");
			xml += printFullTag("entry", dataType.getName());
			if (dataType.isList()) {
				xml += printFullTag("entry", "1 .. n");
			} else {
				xml += printFullTag("entry", "1");
			}
			xml += printFullTag("entry", param.getDescription());
			xml += printEndTag("row");
			xml += printTableBottom();
			xml += printDataType(dataType, true);
			xml += printSample(dataType);
		} else {
			xml += printFullTag("title", "Structure of the request body");
			xml += printFullTag("para", "The request body is not used for this operation.");
		}
		xml += printEndTag("simplesect");

		return xml;
	}

	private String printResponse(String responseClass) {
		DocDataType dataType = dataTypes.get(responseClass);
		String xml = printStartTag("simplesect");
		xml += printFullTag("title", "Structure of the response");
		xml += printFullTag("para", "If successful, the call returns HTTP status 200 OK");
		if (dataType != null) {
			xml += printFullTag("para", "The response body contains the following data:");
			xml += printDataType(dataType, false);
		} else {
			xml += printFullTag("para", "The response body is not used in this operation");
		}
		xml += printEndTag("simplesect");
		return xml;
	}

	private String printDataType(DocDataType dataType, boolean request) {
		String xml = printStartTag("para");
		xml += printTableTop("Structure of the " + dataType.getName() + " element", "2*,1*,1*,3*", "Property", "Type", "Cardinality",
				"Description");
		for (DocProperty property : dataType.getProperties()) {
			xml += printStartTag("row");
			xml += printFullTag("entry", property.getName());
			xml += printFullTag("entry", property.getType());
			String cardinality = property.isRequired() ? "1" : "0";
			if (property.isList()) {
				cardinality += " .. n";
			} else if (cardinality == "0") {
				cardinality += " .. 1";
			}
			xml += printFullTag("entry", cardinality);
			xml += printFullTag("entry", property.getDescription());
			xml += printEndTag("row");
		}
		xml += printTableBottom();
		xml += printEndTag("para");
		for (DocProperty property : dataType.getProperties()) {
			DocDataType propertyDataType = getDataType(property);
			if (propertyDataType != null) {
				xml += printDataType(propertyDataType, request);
			}
		}
		return xml;
	}

	private String printSample(DocDataType dataType) {
		String xml = printStartTag("para");
		xml += printStartTag("example");
		xml += printFullTag("title", "Sample request");
		xml += printStartTagWithAttributes("programlisting", "language=\"json\"");
		xml += printJsonSample(dataType);
		xml += printEndTag("programlisting");
		xml += printEndTag("example");
		xml += printEndTag("para");
		return xml;
	}

	private String printJsonSample(DocDataType dataType) {
		List<String> jsonList = new ArrayList<String>();
		for (DocProperty property : dataType.getProperties()) {
			if (property.hasSample()) {
				String json = "\"" + property.getName() + "\" : ";
				if (property.getSample().length == 1) {
					json += "\"" + property.getSample()[0] + "\"";
				} else {
					json += "[\"" + StringUtils.join(property.getSample() , "\",\"") + "\"]";
				}
				jsonList.add(json);
			}
			DocDataType propertyDataType = getDataType(property);
			if (propertyDataType != null) {
				String sample = printJsonSample(propertyDataType);
				if (StringUtils.isNotBlank(sample) && StringUtils.isNotBlank(property.getName())) {
					if (property.getType().startsWith("List[")) {
						jsonList.add("\"" + property.getName() + "\" : [" + sample + "]");
					} else {
						jsonList.add("\"" + property.getName() + "\" : " + sample);
					}
				} else {
					jsonList.add(sample);
				}
			}
		}
		String json = StringUtils.join(jsonList, ",");
		if (dataType.isList()) {
			json = "[" + json + "]";
		} else {
			json = "{" + json + "}";
		}

		try {
			return JsonWriter.formatJson(json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DocDataType getDataType(DocProperty property) {
		return dataTypes.get(property.getType().replaceAll("List\\[", "").replaceAll("\\]", ""));
	}

	private String printCommonErrorList(Set<DocError> errorList, String section) {
		if (errorList.isEmpty()) {
			return "";
		}
		String xml = printStartTag(section);
		xml += printFullTag("title", "Error codes and descriptions");
		xml += printFullTag("para", "The following error codes may occur during execution");
		xml += printTableTop("List of possible errors", "1*,3*,2*", "Http Status", "Type", "Description");
		for (DocError error : errorList) {
			xml += printStartTag("row");
			xml += printFullTag("entry", "" + error.getHttpStatus());
			xml += printFullTag("entry", "" + error.getErrorCode());
			xml += printFullTag("entry", "" + error.getDescription());
			xml += printEndTag("row");
		}
		xml += printTableBottom();
		xml += printEndTag(section);
		return xml;
	}

	private String printTableTop(String tableTitle, String columnWidths, String... captions) {
		String xml = printStartTag("table");
		xml += printFullTag("title", tableTitle);
		xml += "<?dbfo keep-together=\"auto\" ?>";
		xml += printStartTagWithAttributes("tgroup", "cols=\"" + captions.length + "\"");
		String[] widths = columnWidths.split(",");
		for (int i = 0; i < captions.length; i++) {
			xml += printShortTagWithAttributes("colspec", "colname=\"" + captions[i].toLowerCase() + "\" colwidth=\"" + widths[i] + "\"");
		}
		xml += printStartTag("thead");
		xml += printStartTag("row");
		for (int i = 0; i < captions.length; i++) {
			xml += printFullTag("entry", captions[i]);
		}
		xml += printEndTag("row");
		xml += printEndTag("thead");
		xml += printStartTag("tbody");
		return xml;
	}

	private String printTableBottom() {
		String xml = printEndTag("tbody");
		xml += printEndTag("tgroup");
		xml += printEndTag("table");
		return xml;
	}

	private String printShortTagWithAttributes(String name, String attributes) {
		return "<" + name + " " + attributes + "/>";
	}

	private String printShortTag(String name) {
		return "<" + name + "/>";
	}

	private Integer getYearFromDate() {
		return new Integer(new SimpleDateFormat("yyyy").format(new Date()));
	}

	private String printStartTag(String name) {
		return "<" + name + ">";
	}

	private String printStartTagWithAttributes(String name, String attributes) {
		return "<" + (name + " " + attributes).trim() + ">";
	}

	private String printEndTag(String name) {
		return "</" + name + ">";
	}

	private String printFullTag(String name, String value) {
		return "<" + name + ">" + value + "</" + name + ">";
	}

	private String printVersion() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	}

	public static String prettyFormat(String input, int indent) {
		try {
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			return xmlOutput.getWriter().toString();
		} catch (Exception e) {
			throw new RuntimeException(e); // simple exception handling, please review it
		}
	}

}
