package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import org.junit.jupiter.api.Test;

import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_0A;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_41;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_43;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_44;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_45;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_46;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_47;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_4A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TwoDDocDataTest {

    @Test
    void should_parse_2ddoc_data() {
        TwoDDocData data = TwoDDocData.parse("431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D");

        assertAll(
                () -> assertThat(data.get(ID_0A)).isNull(),
                () -> assertThat(data.get(ID_41)).isEqualTo("42000"),
                () -> assertThat(data.get(ID_43)).isEqualTo("1,5"),
                () -> assertThat(data.get(ID_44)).isEqualTo("4237A18611332"),
                () -> assertThat(data.get(ID_45)).isEqualTo("2021"),
                () -> assertThat(data.get(ID_46)).isEqualTo("DOE JOHN"),
                () -> assertThat(data.get(ID_47)).isEqualTo("1267027554499"),
                () -> assertThat(data.get(ID_4A)).isEqualTo("31042022")
        );
    }

}