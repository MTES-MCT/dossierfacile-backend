package fr.dossierfacile.api.pdfgenerator.test.utils;

import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

public class templateTest {

 public static void main(String[] args) throws IOException {


   PDDocument templatePdf = PDDocument.load(templateTest.class.getResourceAsStream("/page1.pdf"));
   PDDocument mainDocument = new PDDocument();     

   PDPage myPage = new PDPage();
    mainDocument.addPage(myPage);
   PDPageContentStream contentStream = new PDPageContentStream(mainDocument, myPage, PDPageContentStream.AppendMode.APPEND.APPEND, true);

   contentStream.beginText();
   // Some text
  // Table 1 (Depending on table 1 size, pdf pages will increase) 

  contentStream.endText();
  contentStream.close();

  // Process of imposing a layer begins here
  PDPageTree destinationPages = mainDocument.getDocumentCatalog().getPages();

  LayerUtility layerUtility = new LayerUtility(mainDocument);

  PDFormXObject firstForm = layerUtility.importPageAsForm(templatePdf, 0);

  AffineTransform affineTransform = new AffineTransform();
     affineTransform.scale(0.5, 0.5);
     PDPage destPage = destinationPages.get(0);

  layerUtility.wrapInSaveRestore(destPage);
  layerUtility.appendFormAsLayer(destPage, firstForm, affineTransform, "external page");

  mainDocument.save("target/result.pdf");
  mainDocument.close();
 }
}