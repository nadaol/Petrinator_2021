package org.petrinator.editor.filechooser;

import org.petrinator.petrinet.Document;
import org.petrinator.petrinet.Marking;
import org.petrinator.petrinet.PetriNet;
import org.petrinator.petrinet.xml.DocumentExporter;
import org.petrinator.petrinet.xml.DocumentImporter;
import org.petrinator.util.GraphicsTools;
import javax.swing.*;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Leandro Asson on 12/4/2017.
 */
public class PipePnmlFileType extends FileType {

    @Override
    public String getName() {
        return "PNML Pipe Dialect";
    }

    @Override
    public String getExtension() {
        return "xml";
    }

    @Override
    public Icon getIcon() {
        final Icon icon = GraphicsTools.getIcon("pneditor/filechooser/pnml.gif");
        return icon;
    }

    @Override
    public void save(Document document, File file) throws FileTypeException {
        try {
            final InputStream xslt = getClass().getResourceAsStream("/xslt/pipe-export.xslt");
            PetriNet petriNet = document.petriNet;
            Marking initialMarking = petriNet.getInitialMarking();
            new DocumentExporter(document, initialMarking).writeToFileWithXslt(file, xslt);
        } catch (FileNotFoundException ex) {
            throw new FileTypeException(ex.getMessage());
        } catch (JAXBException ex) {
            if (!file.exists()) {
                throw new FileTypeException("File not found.");
            } else if (!file.canRead()) {
                throw new FileTypeException("File can not be read.");
            } else {
                throw new FileTypeException("Selected file is not compatible.");
            }
        } catch (IOException ex) {
            throw new FileTypeException(ex.getMessage());
        } catch (TransformerException ex) {
            throw new FileTypeException(ex.getMessage());
        }
    }

    @Override
    public Document load(File file) throws FileTypeException {
        try {
            final InputStream xslt = getClass().getResourceAsStream("/xslt/pipe-import.xslt");
            Document document = new DocumentImporter().readFromFileWithXslt(file, xslt);
            document.petriNet.getRootSubnet().setViewTranslationToCenterRecursively();
            return document;
        } catch (JAXBException ex) {
            if (!file.exists()) {
                throw new FileTypeException("File not found.");
            } else if (!file.canRead()) {
                throw new FileTypeException("File can not be read.");
            } else {
                throw new FileTypeException("Selected file is not compatible.");
            }
        } catch (IOException ex) {
            throw new FileTypeException(ex.getMessage());
        } catch (TransformerException ex) {
            throw new FileTypeException(ex.getMessage());
        }
    }
}