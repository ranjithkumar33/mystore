package com.microapps.ebusiness.mystore.application.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microapps.ebusiness.mystore.application.domain.CustomerDto;
import com.microapps.ebusiness.mystore.application.exception.CustomerNotFoundException;
import com.microapps.ebusiness.mystore.application.service.ActivityService;
import com.microapps.ebusiness.mystore.application.service.CustomerService;
import com.microapps.ebusiness.mystore.application.service.SecurityContext;
import com.microapps.ebusiness.mystore.application.util.CurrencyUtil;
import com.microapps.ebusiness.mystore.application.util.CustomerNameUtils;
import com.microapps.ebusiness.mystore.application.util.DateUtil;
import com.microapps.ebusiness.mystore.application.util.UIValidationUtils;
import com.microapps.ebusiness.mystore.application.util.ViewTemplateConstants;
import com.microapps.ebusiness.mystore.application.view.CustomerView;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HomeController extends BaseController implements Initializable, Routeable{
	
	 private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());
	
	public HomeController() {
		cs = new CustomerService();
		as = new ActivityService();
	}
	
	private CustomerService cs;
	private ActivityService as;
	
	private Parent rootNode;
	
	@FXML
	private Label userName;
	
	@FXML
	private TextField searchField;
	
	@FXML
	private ProgressIndicator progressIndicator;
	
	@FXML
	private Label progressText;
	
	private boolean hasValidationError;
	
	@FXML
	private Button searchButton;
	
	
	@FXML
    private TableView<CustomerView> customersTable;
	
    @FXML
    private TableColumn<CustomerView, LocalDate> dateColumn;
    
    @FXML
    private TableColumn<CustomerView, String> customerNameColumn;
    
    @FXML
    private TableColumn<CustomerView, Double> amountColumn;
    
    @FXML
    private Label totalActiveCustomers;
    
    @FXML
    private Label totalSale;
    
    private ObservableList<CustomerView> customers = FXCollections.observableArrayList();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		searchField.setPromptText("Enter Mobile or Card number");
		searchField.textProperty().addListener(new ChangeListener<String>() {
	        @Override
	        public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
	            if (searchField.getText().length() > UIValidationUtils.MAX_LENGTH_OF_CARD_NUMBER) {
	                String s = searchField.getText().substring(0, UIValidationUtils.MAX_LENGTH_OF_CARD_NUMBER);
	                searchField.setText(s);
	            }
	            if(hasValidationError) {
	            	 progressText.setTextFill(Color.web("#948282"));
    				 progressText.setText("");
    				 searchButton.setDisable(true);
    				 hasValidationError=!hasValidationError;
	            }else {
	            	 searchButton.setDisable(false);
	            }
	        }
	    });
		
		searchField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
	        if (!newValue) { //when focus lost
	            if(!searchField.getText().matches(UIValidationUtils.REGEX_FOR_NUMBER)){
	            	 hasValidationError = true;
	            	 progressText.setTextFill(Color.web("#eb3144"));
    				 progressText.setText(UIValidationUtils.CUSTOMER_SEARCH_ERROR_INVALID_CHARACTER);
    				 searchButton.setDisable(true);
	            }
	        }

	    });
		
	}


	public void initData() {
		userName.setText(SecurityContext.getSecurityContext().getAuthenticatedUser().getName());
		setCustomerTableView();
	}
	
	private void setCustomerTableView() {
		try {
			cs.getAllCustomers().forEach(item -> {
				customers.add(new CustomerView(item.getId(), CustomerNameUtils.getNameWithTitle(item), item.getTotalPurchaseAmount(), item.getCreatedOn().toLocalDateTime(), item.isAuthenticated()));
			});
			
			customersTable.setItems(customers);
			
			dateColumn.setCellValueFactory(cellData -> cellData.getValue().getDate());
			customerNameColumn.setCellValueFactory(cellData -> cellData.getValue().getName());
			amountColumn.setCellValueFactory(cellData -> cellData.getValue().getAmount().asObject());
			
			amountColumn.setCellFactory(column -> {
				return new TableCell<CustomerView, Double>() {
			        @Override
			        protected void updateItem(Double item, boolean empty) {
			        	
			        	if (item == null || empty) {
			        		setText("");
			            }else {
			            	setText(CurrencyUtil.getFormattedAmount(item));
			            }
			        }
			    };
			});
			
			customerNameColumn.setCellFactory(column -> {
				return new TableCell<CustomerView, String>() {
			        @Override
			        protected void updateItem(String item, boolean empty) {
			        	
			        	if (item == null || empty) {
			        		setText("");
			            }else {
			            	
			            	Hyperlink link = new Hyperlink();
			            	link.setText(item);
			            	link.setOnAction(new EventHandler<ActionEvent>() {
			            	    @Override
			            	    public void handle(ActionEvent e) {
			            	    	CustomerView cv = (CustomerView)getTableRow().getItem();
			            	    	
			            	    	Task<CustomerDto> findCustomerTask = new Task<CustomerDto>() {

			            				@Override
			            				protected CustomerDto call() throws Exception {
			            					CustomerDto c = null;
			            					c = cs.getCustomerById(cv.getId().get());
			            					/*try {
			            						c = cs.getCustomerById(cv.getId().get());
			            					} catch (CustomerNotFoundException e) {
			            						throw e;
			            					}*/
			            					return c;
			            				}
			            				
			            			};
			            			findCustomerTask.setOnScheduled(value ->{
			            				
			            				CustomerDto c = findCustomerTask.getValue();
				            			
			            				Router.getRouter().route("view-customer").showView((getStage(e)));
			            			});
			            			 new Thread(findCustomerTask).start();
			            	    }
			            	});
			            	HBox hb= new HBox();
			            	hb.setSpacing(10);
			            	hb.getChildren().add(link);
			            	if(item.contains("NoName")) {
			            		Image img = new Image(HomeController.class.getResourceAsStream("alert-bell-notification.png"));
			            		ImageView iv = new ImageView(img);
			            		iv.setFitHeight(20);
			            		iv.setFitWidth(20);
			            		hb.getChildren().add(iv);
			            	}
			        		setGraphic(hb);
			            }
			        }//BaseController.class.getResource(ViewTemplateConstants.BUSINESS_REGISTRATION_VIEW)
			    };
			});
			
			dateColumn.setCellFactory(column -> {
				return new TableCell<CustomerView, LocalDate>() {
			        @Override
			        protected void updateItem(LocalDate item, boolean empty) {
			        	
			        	if (item == null || empty) {
			        		setText("");
			            }else {
			            	setText(DateUtil.toString(item));
			            }
			        }
			    };
			});
			NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
			totalActiveCustomers.setText("Total Customers : "+formatter.format(customers.size()));
			totalSale.setText("Total Sale : "+ CurrencyUtil.getFormattedAmount(as.getTotalSaleAmount()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void handleLogout(ActionEvent event) {
		SecurityContext.getSecurityContext().removeUserFromContext();
		Router.getRouter().route("logout").showView((getStage(event)));
	}
	
	@FXML
	private void reload(ActionEvent event) {
		Router.getRouter().route("home").showView((getStage(event)));
	}
	
	@FXML
	private void createCustomer(ActionEvent event) {
		Router.getRouter().route("new-customer").showView(getStage(event));
	}
	
	@FXML
	private void handleSearch(ActionEvent event) {
		
		if(null == searchField.getText() || searchField.getText().trim().length() == 0) {
			hasValidationError = true;
			progressText.setTextFill(Color.web("#eb3144"));
			 progressText.setText("Please enter a value");
			 searchButton.setDisable(true);
			 return;
		}
		
		progressIndicator.setVisible(true);
		progressIndicator.setProgress(0);
		progressText.setTextFill(Color.web("#948282"));
		progressText.setText("Searching...");
		
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				try {
					 cs.searchCustomer(searchField.getText());
					} catch (CustomerNotFoundException e) {
						throw e;
					}
				return null;
			}

			
			
		};
		 progressIndicator.progressProperty().unbind();
		 progressIndicator.progressProperty().bind(task.progressProperty());
		 
		 task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent t) {
                        	CustomerDto _c = (CustomerDto) t.getSource().getValue();
                            progressText.setText("");
                            progressIndicator.setVisible(false);
                            progressIndicator.progressProperty().unbind();
                            Router.getRouter().route("view-customer").showView((getStage(event)));
                        }
            });
		 
		 task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {

             @Override
             public void handle(WorkerStateEvent t) {
            	Exception e =  (Exception) t.getSource().getException();
            	if(e instanceof CustomerNotFoundException) {
            		 progressText.setTextFill(Color.web("#eb3144"));
    				 progressText.setText(e.getMessage());
                     progressIndicator.setVisible(false);
                     progressIndicator.progressProperty().unbind();
            	}else if(e instanceof SQLException) {
            		LOGGER.log(Level.SEVERE, "The application closes because of the error");
        	    	Platform.exit();
            	}
            	
             	
             }
           });

            // Start the Task.
            new Thread(task).start();


	}

	@Override
	public void showView(Stage stage) {
		FXMLLoader fxmlLoader = new FXMLLoader(HomeController.class.getResource(ViewTemplateConstants.HOME_VIEW));
        try {
			rootNode = fxmlLoader.load();
			stage.setScene(new Scene(rootNode));
			HomeController hc = fxmlLoader.getController();
			hc.initData();
	        stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	
}
