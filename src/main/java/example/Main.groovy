package example

import Diadoc.Api.DiadocApi
import Diadoc.Api.Proto.CustomDataItemProtos
import Diadoc.Api.Proto.Events.DiadocMessage_GetApiProtos
import Diadoc.Api.Proto.Events.DiadocMessage_PostApiProtos
import Diadoc.Api.Proto.OrganizationProtos
import com.google.protobuf.ByteString
import org.apache.commons.io.IOUtils

class Main {

    static void main(String[] args) throws Exception {
        def config = new ConfigSlurper().parse(new File("config.groovy").text)

        String apiUrl = config.apiUrl
        String apiKey = config.apiKey
        String login = config.login
        String password = config.password
        String data = config.data

        new File(data + "/acts.csv").eachLine { row ->
            if (row.trim()) {
                String fileName, senderInn, recipientInn, actId, actItem, actSum, actDate
                (fileName, senderInn, recipientInn, actId, actItem, actSum, actDate) = row.split("\\;")
                byte[] act = IOUtils.toByteArray(new FileInputStream(data + "/" + fileName));

                DiadocApi api = new DiadocApi(apiKey, apiUrl);
                api.Authenticate(login, password);
                OrganizationProtos.OrganizationList organizations = api.GetMyOrganizations();
                OrganizationProtos.Organization organization = null;
                for (OrganizationProtos.Organization org : organizations.getOrganizationsList()) {
                    if (senderInn.equals(org.getInn())) {
                        organization = org;
                    }
                }
                if (organization == null) {
                    throw new DiadocApiException("sender organization not found");
                }
                if (organization.getBoxesCount() != 1) {
                    throw new DiadocApiException("wrong boxes count");
                }
                OrganizationProtos.Box box = organization.getBoxes(0);

                List<OrganizationProtos.Organization> organizationsByInn = api.GetOrganizationsByInnList(Collections.singletonList(recipientInn));
                organizationsByInn = organizationsByInn.findAll { !it.isRoaming && it.hasOgrn() }
                if (organizationsByInn.size() != 1) {
                    throw new DiadocApiException("no recipient");
                }
                OrganizationProtos.Organization recipient = organizationsByInn.get(0);
                if (recipient.getBoxesCount() != 1) {
                    throw new DiadocApiException("wrong recipient boxes count");
                }
                OrganizationProtos.Box toBox = recipient.getBoxes(0);

                DiadocMessage_GetApiProtos.Message message = api.PostMessage(
                        DiadocMessage_PostApiProtos.MessageToPost.newBuilder()
                                .setFromBoxId(box.getBoxId())
                                .setToBoxId(toBox.getBoxId())
                                .setIsDraft(true)
                                .addAcceptanceCertificates(
                                DiadocMessage_PostApiProtos.AcceptanceCertificateAttachment.newBuilder()
                                        .setSignedContent(
                                        DiadocMessage_PostApiProtos.SignedContent.newBuilder()
                                                .setContent(ByteString.copyFrom(act))
                                )
                                        .setFileName(fileName)
                                        .setDocumentDate(actDate)
                                        .setDocumentNumber(actId)
                                        .setTotal(actSum)
                                        .setNeedRecipientSignature(true)
                                        .setGrounds(actItem)
                                        .addCustomData(
                                        CustomDataItemProtos.CustomDataItem.newBuilder()
                                                .setKey("date-generated")
                                                .setValue(new Date().toString())
                                )
                        )
                                .build()
                );

                System.out.println(message);
            }
        }
    }
}
