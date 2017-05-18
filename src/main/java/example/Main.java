package example;

import Diadoc.Api.DiadocApi;
import Diadoc.Api.Proto.CustomDataItemProtos;
import Diadoc.Api.Proto.Events.DiadocMessage_GetApiProtos;
import Diadoc.Api.Proto.Events.DiadocMessage_PostApiProtos;
import Diadoc.Api.Proto.OrganizationProtos;
import com.google.protobuf.ByteString;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String apiUrl = "https://diadoc-api.kontur.ru/";
        String apiKey = "ihc-00000000-0000-0000-0000-000000000000";
        String login = "username@ihc.ru";
        String password = "password";

        String recipientInn = "7701838266";
        String fileName = "example.pdf";
        byte[] act = IOUtils.toByteArray(new FileInputStream(fileName));
        String actId = "632352";
        String actItem = "Услуги хостинга с 01.01.2000 по 01.02.2000 p12345";
        String actSum = "1000";
        String actDate = "01.02.2000";

        DiadocApi api = new DiadocApi(apiKey, apiUrl);
        api.Authenticate(login, password);
        OrganizationProtos.OrganizationList organizations = api.GetMyOrganizations();
        if (organizations.getOrganizationsCount() != 1) {
            throw new DiadocApiException("wrong organizations count");
        }
        OrganizationProtos.Organization organization = organizations.getOrganizations(0);
        if (organization.getBoxesCount() != 1) {
            throw new DiadocApiException("wrong boxes count");
        }
        OrganizationProtos.Box box = organization.getBoxes(0);

        List<OrganizationProtos.Organization> organizationsByInn = api.GetOrganizationsByInnList(Collections.singletonList(recipientInn));
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
