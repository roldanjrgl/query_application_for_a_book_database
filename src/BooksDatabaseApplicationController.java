import java.sql.SQLException;
import java.util.regex.PatternSyntaxException;

import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;           
import javax.swing.table.TableModel;    
import javax.swing.table.TableRowSorter;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
public class BooksDatabaseApplicationController {
   @FXML private BorderPane borderPane;
   @FXML private TextArea queryTextArea;
   @FXML private TextField filterTextField;
   @FXML private ComboBox<String> predefinedQuery;
   @FXML private TextField predefinedQueryTextField;
   @FXML private TextField addAuthorTextField;
   @FXML private TextField deleteAuthorTextField;

    @FXML private TextField isbnTextField;
    @FXML private TextField titleTextField;
    @FXML private TextField editionNumberTextField;
    @FXML private TextField copyrightTextField;
    @FXML private TextField authorIDTextField;

    private ObservableList<String> options = FXCollections.observableArrayList(
                    "Select All authors",
                    "Select All titles",
                    "Select a specific author",
                    "Select a specific title");
    private String[] predefinedQueryStrings = {
            "Select All authors",
            "Select All titles",
            "Select a specific author",
            "Select a specific title"};

   // database URL, username and password
   private static final String DATABASE_URL = "jdbc:derby:books";
   private static final String USERNAME = "deitel";
   private static final String PASSWORD = "deitel";

   // default query retrieves all data from Authors table
   private static final String DEFAULT_QUERY = "SELECT * FROM authors";
   
   // used for configuring JTable to display and sort data
   private ResultSetTableModel tableModel;
   private TableRowSorter<TableModel> sorter;

    // initialize
   public void initialize() {
      queryTextArea.setText(DEFAULT_QUERY);

      // create ResultSetTableModel and display database table
      try {
//          Initialize ComboBox
          predefinedQuery.setValue("Select All Authors");
          predefinedQuery.setItems(options);
          
         // create TableModel for results of DEFAULT_QUERY
         tableModel = new ResultSetTableModel(DATABASE_URL,            
            USERNAME, PASSWORD, DEFAULT_QUERY);

         // create JTable based on the tableModel    
         JTable resultTable = new JTable(tableModel);

         // set up row sorting for JTable
         sorter = new TableRowSorter<TableModel>(tableModel);
         resultTable.setRowSorter(sorter);             

         // configure SwingNode to display JTable, then add to borderPane
         SwingNode swingNode = new SwingNode();
         swingNode.setContent(new JScrollPane(resultTable));
         borderPane.setCenter(swingNode);

      }
      catch (SQLException sqlException) {
         displayAlert(AlertType.ERROR, "Database Error", 
            sqlException.getMessage());
         tableModel.disconnectFromDatabase(); // close connection  
         System.exit(1); // terminate application
      } 
   }

   private String getPredefinedQueryTextField(){
       return predefinedQueryTextField.getText();
   }

   // spliting predefinedQueryTextField
    private String[] splitPredefinedQueryTextField(){

       return getPredefinedQueryTextField().split("\\s");
    }

    // return query string for partb
    private String getAllTitlesForSpecificAuthor(){
        String[] stringArray = splitPredefinedQueryTextField();
        return "SELECT LastName, FirstName, Title " +
                "FROM Authors INNER JOIN AuthorISBN " +
                "ON Authors.AuthorID=AuthorISBN.AuthorID " +
                "INNER JOIN Titles " +
                "ON AuthorISBN.ISBN=Titles.ISBN " +
                "WHERE LastName = '"+ stringArray[0]
                + "' AND FirstName = '" + stringArray[1] + "'";
    }

    private String getAllAuthorsForSpecificTitle(){
        return "SELECT Title, LastName, FirstName " +
                "FROM Authors " +
                "INNER JOIN AuthorISBN "+
                "ON Authors.AuthorID=AuthorISBN.AuthorID " +
                "INNER JOIN Titles " +
                "ON AuthorISBN.ISBN=Titles.ISBN " +
                "WHERE Title = '" + getPredefinedQueryTextField() + "'"+
                " ORDER BY LastName, FirstName ASC";
    }

   // query the database and display results in JTable
   @FXML
   private void submitQueryButtonPressed(ActionEvent event) {
      // perform a new query
      try {
          if(predefinedQuery.getValue().toString() == predefinedQueryStrings[0]){
              tableModel.setQuery("SELECT * FROM authors");
              System.out.println(predefinedQuery.getValue());
          }
          else if(predefinedQuery.getValue().toString() == predefinedQueryStrings[1]){
              tableModel.setQuery("SELECT * FROM titles");
              System.out.println(predefinedQuery.getValue());
          }
          else if(predefinedQuery.getValue().toString() == predefinedQueryStrings[2]){
              tableModel.setQuery(getAllTitlesForSpecificAuthor());
              System.out.println(predefinedQuery.getValue());
          }
          else if(predefinedQuery.getValue().toString() == predefinedQueryStrings[3]){
              tableModel.setQuery(getAllAuthorsForSpecificTitle());
              System.out.println(predefinedQuery.getValue());
          }
          else{
              tableModel.setQuery(queryTextArea.getText());
          }
      }
      catch (SQLException sqlException) {
         displayAlert(AlertType.ERROR, "Database Error", 
            sqlException.getMessage());
         
         // try to recover from invalid user query 
         // by executing default query
         try {
            tableModel.setQuery(DEFAULT_QUERY);
            queryTextArea.setText(DEFAULT_QUERY);
         } 
         catch (SQLException sqlException2) {
            displayAlert(AlertType.ERROR, "Database Error", 
               sqlException2.getMessage());
            tableModel.disconnectFromDatabase(); // close connection  
            System.exit(1); // terminate application
         } 
      } 
   }

   // apply specified filter to results
   @FXML
   private void applyFilterButtonPressed(ActionEvent event) {
      String text = filterTextField.getText();

      if (text.length() == 0) {
         sorter.setRowFilter(null);
      }
      else {
         try {
            sorter.setRowFilter(RowFilter.regexFilter(text));
         } 
         catch (PatternSyntaxException pse) {
            displayAlert(AlertType.ERROR, "Regex Error", 
               "Bad regex pattern");
         }
      }
   }

   @FXML
   private void addAuthorBottomPressed(ActionEvent event){
       String newAuthor = addAuthorTextField.getText();
       String[] newAuthorStringArray = newAuthor.split("\\s");

       // perform a new query
       try {
           tableModel.executeUpdate("INSERT INTO authors (firstName,lastName) "
                                + "VALUES ('" + newAuthorStringArray[1] +
                   "', " + "'"+newAuthorStringArray[0]+ "' )");
       }
       catch (SQLException sqlException) {
           displayAlert(AlertType.ERROR, "Database Error",
                   sqlException.getMessage());

           // try to recover from invalid user query
           // by executing default query
           try {
               tableModel.setQuery(DEFAULT_QUERY);
               queryTextArea.setText(DEFAULT_QUERY);
           }
           catch (SQLException sqlException2) {
               displayAlert(AlertType.ERROR, "Database Error",
                       sqlException2.getMessage());
               tableModel.disconnectFromDatabase(); // close connection
               System.exit(1); // terminate application
           }
       }
   }

   @FXML
   private void deleteAuthorBottomPressed(ActionEvent event){
       String deletedAuthor = deleteAuthorTextField.getText();
       String[] deletedAuthorStringArray = deletedAuthor.split("\\s");

       // perform a new query
       try {
           tableModel.executeUpdate("DELETE FROM Authors "
                   + "WHERE LastName = '" + deletedAuthorStringArray[0] + "' " +
                   "AND FirstName = " + "'" + deletedAuthorStringArray[1]+ "'");
       }
       catch (SQLException sqlException) {
           displayAlert(AlertType.ERROR, "Database Error",
                   sqlException.getMessage());

           // try to recover from invalid user query
           // by executing default query
           try {
               tableModel.setQuery(DEFAULT_QUERY);
               queryTextArea.setText(DEFAULT_QUERY);
           }
           catch (SQLException sqlException2) {
               displayAlert(AlertType.ERROR, "Database Error",
                       sqlException2.getMessage());
               tableModel.disconnectFromDatabase(); // close connection
               System.exit(1); // terminate application
           }
       }
   }
   
   @FXML
   private void addNewTitleBottomPressed(ActionEvent event){


       try {
           int editionNumber = Integer.parseInt(editionNumberTextField.getText());
           tableModel.executeUpdate("INSERT INTO titles (isbn,title,editionNumber,copyright) "
                   + "VALUES ('" + isbnTextField.getText() + "', '" + titleTextField.getText()+
                   "', '" + editionNumber  + "', '" + copyrightTextField.getText()+ "' )");
       }
       catch (SQLException sqlException) {
           displayAlert(AlertType.ERROR, "Database Error",
                   sqlException.getMessage());
           // try to recover from invalid user query
           // by executing default query
           try {
               tableModel.setQuery(DEFAULT_QUERY);
               queryTextArea.setText(DEFAULT_QUERY);
           }
           catch (SQLException sqlException2) {
               displayAlert(AlertType.ERROR, "Database Error",
                       sqlException2.getMessage());
               tableModel.disconnectFromDatabase(); // close connection
               System.exit(1); // terminate application
           }
       }
   }

    @FXML
    private void addToAuthorISBN(ActionEvent event){
        try {
            int authorID = Integer.parseInt(authorIDTextField.getText());
           tableModel.executeUpdate("INSERT INTO authorISBN (authorID,isbn) "
                   + "VALUES ('" + authorID + authorIDTextField.getText() +
                   "', '" + isbnTextField.getText()+ "')");
        }
        catch (SQLException sqlException) {
            displayAlert(AlertType.ERROR, "Database Error",
                    sqlException.getMessage());
        }

    }

   // displasy an Alert dialog
   private void displayAlert(AlertType type, String title, String message) {
      Alert alert = new Alert(type);
      alert.setTitle(title);
      alert.setContentText(message);
      alert.showAndWait();
   }

}

