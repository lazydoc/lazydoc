package org.lazydoc.model;

import org.apache.commons.lang3.StringUtils;

public class DocSubDomain extends DocDomain {

	private String subDomain = "";
	private String subDomainShortDescription = "";

	public String getSubDomain() {
		return subDomain;
	}

	public void setSubDomain(String subDomain) {
		this.subDomain = subDomain;
	}

	public String getSubDomainShortDescription() {
		if (StringUtils.isNotBlank(subDomainShortDescription)) {
			return subDomainShortDescription;
		}
		return subDomain;
	}

	public void setSubDomainShortDescription(String subDomainShortDescription) {
		this.subDomainShortDescription = subDomainShortDescription;
	}

	@Override
	public String getDomain() {
		return domain + "-" + subDomain;
	}

}
