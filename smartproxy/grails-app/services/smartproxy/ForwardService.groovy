package smartproxy

import org.chip.rdf.Demographics;
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovyx.net.http.RESTClient;
import static groovyx.net.http.ContentType.URLENC

class ForwardService {

    static transactional = false

	final static Map personIdMap

	def milleniumService
		
	static{
		personIdMap = new HashMap()
		personIdMap.put ('14109736','15649502')
		personIdMap.put ('14109737','15649505')
		personIdMap.put ('14109738','15649508')
		personIdMap.put ('14109748','15649511')
	}
	
    def createURL(personId, domain, initialApp) {
		personId = extractId(personId)
        initialApp = initialApp ? ("?initial_app="+initialApp) : ""

		if(domain=='demo' || domain==null){
            if (mapPersonId(personId)) {
			    personId=mapPersonId(personId)
            }
		}

        def token = ConfigurationHolder.config.oauth.smartEmr.token
        def secret= ConfigurationHolder.config.oauth.smartEmr.secret
        def apiBase= ConfigurationHolder.config.oauth.smartEmr.apiBase
		
		def created
		def getUrl
		try{
	        def smartClient= new RESTClient(apiBase)
	
	        // Instantiate as a consumer for 2-legged OAuth calls (no access tokens)
	        smartClient.auth.oauth(token, secret, "", "");
	
	
	        created = smartClient.post(path: "/records/create/proxied",
	                                        body : [record_id:personId,
	                                        record_name:getNameForPersonId(personId)],
	                                        requestContentType : URLENC )
	        if(created.status == 200){
				getUrl = smartClient.get(path:"/records/"+personId+"/generate_direct_url")
	        }
		}catch(URISyntaxException urise){
			throw new Exception("Invalid smart server URI: "+ apiBase, urise)
		}catch(IOException ioe){
			throw new Exception("Error communicating with SMART server at: "+ apiBase, ioe)
		}

		if (created.status!=200 || getUrl.status!=200){
			log.error("Failed to get 'forward to' URL for record: "+ personId + " from smart emr at: "+apiBase)
			throw new Exception("SMART Emr url generation failure")
		}
		

        def forwardToURL = getUrl.data.readLine() + initialApp
		return forwardToURL
    }
	
	def mapPersonId(personId) {
		personIdMap.get personId
	}
	
	def extractId(String incomingId){
		def index = incomingId.indexOf(".")
		if (index >-1){
			return incomingId.substring(0, index)
		}
		return incomingId
	}

	def getNameForPersonId(personId){
		def  record = milleniumService.makeCall('demographics', personId)
		if(record=='Error'){
			log.warn("Unable to get Name for the patient")
			return ""
		}
		return record.getGivenName()+" "+record.getFamilyName()
	}

}
