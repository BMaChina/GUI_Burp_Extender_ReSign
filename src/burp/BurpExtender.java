package burp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import java.awt.Component;
import java.awt.Cursor;
import java.io.PrintWriter;
import burp.CAESOperator; //AES�ӽ����㷨��ʵ����
import burp.IParameter;
import burp.CUnicodeDecoder;


public class BurpExtender implements IBurpExtender, IHttpListener, ITab, IContextMenuFactory
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
	public JCheckBox chckbxProxy;
	public JCheckBox chckbxScanner;
	public JCheckBox chckbxIntruder;
	public JCheckBox chckbxRepeater;
	public static JTextField textFieldDomain;
	public static JTable table;
	public JTextField textFieldSecretKey;
	public JCheckBox chckbxAppendToEnd;
	public JCheckBox chckbxSameAsPara;
	public JTextField textFieldConnector;
	public JTextArea textAreaFinalString;
	public JCheckBox chckbxMD5;
	public JCheckBox chckbxNewCheckBox_3;
	public JTextArea textAreaSign;
	public JPanel contentPane;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField textFieldBlackList;
	
	
	
	public String secretKey = null;
	public int sortedColumn;
	public SortOrder sortedMethod;
	public String howDealKey = ""; //sameAsPara  or appendToEnd
	String signPara = null; //the key name of sign parameter
	private JTextField textFieldSign;
    
	
    // implement IBurpExtender
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {//�����ز����ʱ�򣬻��������ķ�����
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	//PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true); ����д���Ƕ��������ʵ����������ı��������µı���������֮ǰclass�е�ȫ�ֱ����ˡ�
    	stdout.println("ReSign v1.0 by bit4    https://github.com/bit4woo");
    	//System.out.println("test"); ���������burp��
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("ReSign v1.0 by bit4"); //�������
        callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
        callbacks.registerContextMenuFactory(this);
        addMenuTab();
    }

    @Override
    public void processHttpMessage(int toolFlag,boolean messageIsRequest,IHttpRequestResponse messageInfo)
    {
    	if (toolFlag == (toolFlag&checkEnabledFor())){ //��ͬ��toolflag�����˲�ͬ��burp��� https://portswigger.net/burp/extender/api/constant-values.html#burp.IBurpExtenderCallbacks
    		if (messageIsRequest){ //����������д���
    			
    			//��ȡ���ֲ�������Ϣ��ķ����������£��޷����֣�body��header��paramater
    			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo); //����Ϣ����н��� 
    			//the method of get header
    			List<String> headers = analyzeRequest.getHeaders(); //��ȡhttp����ͷ����Ϣ�����ؿ��Կ�����һ��python�е��б�java���ǽз���ʲô�ģ���ûŪ���
    			//the method of get body
    			int bodyOffset = analyzeRequest.getBodyOffset();
    			byte[] byte_Request = messageInfo.getRequest();
    			String request = new String(byte_Request); //byte[] to String
                String body = request.substring(bodyOffset);
                byte[] byte_body = body.getBytes();  //String to byte[]
    			//the method of get parameter
                List<IParameter> paras = analyzeRequest.getParameters();
                
                
                //����ͼ������ȡ�������
                Map<String,String> paraList = getPara(analyzeRequest);//��ȡ��Ҫ����sign����Ĳ������Զ����getPara�ų���blacklist�еĲ���
                signPara = textFieldSign.getText();
                byte getSignParaType = getSignParaType(analyzeRequest);
                
                
                //�ж�һ�������Ƿ����ļ��ϴ�������//ͼ�ν���û���ⲿ�ֹ��ܣ���ʱ����
    			boolean isFileUploadRequest =false;
//    			for (String header : headers){
//    				//stdout.println(header);
//    				if (header.toLowerCase().indexOf("content-type")!=-1 && header.toLowerCase().indexOf("boundary")!=-1){//ͨ��httpͷ�е������ж���������Ƿ����ļ��ϴ�������
//    					isFileUploadRequest = true;
//    				}
//    			}
    			//*******************recalculate sign**************************//
    			if (isFileUploadRequest == false){ //��ĳЩ�����������������������
    				if (getHost(analyzeRequest).endsWith(getHostFromUI()) && signPara != null && secretKey !=null && getSignParaType !=-1){//���ͼ������ϵĸ��ֲ��������뱸�˲Ž��С�
    	    			byte[] new_Request = messageInfo.getRequest();
    	    			String str = combineString(paraList);
    	    			CMD5 getMD5 = new CMD5();
    					String newSign = getMD5.GetMD5Code(str);
    		    		//stdout.println("New Sign:"+newSign); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
        				//���°��ķ�������
        				//���²���
        				IParameter newPara = helpers.buildParameter(signPara, newSign, getSignParaType); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
        				//IParameter newPara = helpers.buildParameter(key, aesvalue, PARAM_BODY); //Ҫʹ�����PARAM_BODY �ǲ�����Ҫ��ʵ����IParameter�ࡣ
        				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ�������������Ƿ���һupdateParameter
        				// new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
        				
//            				//����������json���ݸ�ʽ�е�ʱ����Ҫ�õ����·�����
//            				//�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ���Ҫ�������еĲ���Ҳ��Ҫʹ�����µķ�����
//        	    			JSONObject jsonObject = JSON.parseObject(body);
//        	    			JSONObject header = jsonObject.getJSONObject("header");
//        	    			header.replace("sign", sign);
//        	    			jsonObject.replace("header", header);
//        	    			body = JSON.toJSONString(jsonObject);
        				
    	    			messageInfo.setRequest(new_Request);//���������µ������
    	    			stdout.println(new String(messageInfo.getRequest()));
    	    			stdout.print("\r\n");
    	    			/* to verify the updated result
    	    			for (IParameter para : helpers.analyzeRequest(messageInfo).getParameters()){
    	    				stdout.println(para.getValue());
    	    			}
    	    			*/

    				}
    			}


    			
    		}  		
    	}
    		
    }

    
	public void CGUI() {
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));

		
		JPanel enableConfigPanel = new JPanel();
		enableConfigPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		FlowLayout flowLayout = (FlowLayout) enableConfigPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(enableConfigPanel, BorderLayout.NORTH);
		
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(null);
		enableConfigPanel.add(panel_3);
		panel_3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel enableFor = new JLabel("Enable For :");
		panel_3.add(enableFor);
		
		chckbxProxy = new JCheckBox("Proxy");
		panel_3.add(chckbxProxy);
		
		chckbxScanner = new JCheckBox("Scanner");
		panel_3.add(chckbxScanner);
		
		chckbxIntruder = new JCheckBox("Intruder");
		panel_3.add(chckbxIntruder);
		
		chckbxRepeater = new JCheckBox("Repeater");
		chckbxRepeater.setSelected(true);
		panel_3.add(chckbxRepeater);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		
		JLabel lblNewLabel = new JLabel("ReSign v1.0 by bit4    https://github.com/bit4woo");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		panel_1.add(lblNewLabel);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_5 = new JPanel();
		panel.add(panel_5, BorderLayout.NORTH);
		panel_5.setLayout(new GridLayout(0, 1, 0, 0));
		
		JLabel lblDomain = new JLabel("Domain:");
		panel_5.add(lblDomain);
		
		textFieldDomain = new JTextField();
		panel_5.add(textFieldDomain);
		textFieldDomain.setColumns(20);
		
		JLabel lblParas = new JLabel("Parameters:(Click Table Header To Sort)");
		panel_5.add(lblParas);
		
		JScrollPane panel_6 = new JScrollPane();
		panel_6.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel.add(panel_6, BorderLayout.CENTER);
		
		table = new JTable();
		table.setColumnSelectionAllowed(true);
		table.setCellSelectionEnabled(true);
		table.setSurrendersFocusOnKeystroke(true);
		table.setFillsViewportHeight(true);
		table.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		DefaultTableModel tableModel = new DefaultTableModel(
				new Object[][] {
					//{null, null},
				},
				new String[] {
					"Key", "Value"
				});
		RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);
		panel_6.setViewportView(table);
		table.setModel(tableModel);
		
		JPanel panel_7 = new JPanel();
		panel.add(panel_7, BorderLayout.EAST);
		GridBagLayout gbl_panel_7 = new GridBagLayout();
		gbl_panel_7.columnWidths = new int[]{93, 0};
		gbl_panel_7.rowHeights = new int[]{23, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel_7.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_7.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_7.setLayout(gbl_panel_7);
		
		JButton btnNewButton = new JButton("Remove");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
				if (table.getSelectedRow() != -1){
					tableModel.removeRow(table.getSelectedRow());//���һ��ɾ�����У�
				}
			}
		});
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 0;
		panel_7.add(btnNewButton, gbc_btnNewButton);
		
		JButton btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.addRow(new Object[]{"k","v"});
			}
		});
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnAdd.gridx = 0;
		gbc_btnAdd.gridy = 1;
		panel_7.add(btnAdd, gbc_btnAdd);
		
		JButton btnNewButton_1 = new JButton("Add To Black List");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String blackListString = textFieldBlackList.getText();
				List<String> blackList = Arrays.asList(blackListString.split(" "));
				if (table.getSelectedRow() != -1){
					String x = table.getValueAt(table.getSelectedRow(), 0).toString();
					if (!blackList.contains(x) & x != "" & x != null)
						blackListString +=" "+x;
				}
				textFieldBlackList.setText(blackListString);
			}
		});
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_1.gridx = 0;
		gbc_btnNewButton_1.gridy = 2;
		panel_7.add(btnNewButton_1, gbc_btnNewButton_1);
		
		
		JButton button = new JButton("Show Final String");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = combineString(getParaFromTable());
				textAreaFinalString.setText(str);
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 4;
		panel_7.add(button, gbc_button);
		
		
		JButton btnMarkAsSign = new JButton("Mark As Sign Para");
		btnMarkAsSign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (table.getSelectedRow() != -1){
					signPara = table.getValueAt(table.getSelectedRow(), 0).toString();
					textFieldSign.setText(signPara);
					
					//add to blacklist
					String blackListString = textFieldBlackList.getText();
					List<String> blackList = Arrays.asList(blackListString.split(" "));
					if (!blackList.contains(signPara) & signPara != "" & signPara != null)
						blackListString +=" "+signPara;
					textFieldBlackList.setText(blackListString);
				}
			}
		});
		GridBagConstraints gbc_btnMarkAsSign = new GridBagConstraints();
		gbc_btnMarkAsSign.insets = new Insets(0, 0, 5, 0);
		gbc_btnMarkAsSign.gridx = 0;
		gbc_btnMarkAsSign.gridy = 3;
		panel_7.add(btnMarkAsSign, gbc_btnMarkAsSign);
		
		textFieldSign = new JTextField();
		GridBagConstraints gbc_textFieldSign = new GridBagConstraints();
		gbc_textFieldSign.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldSign.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldSign.gridx = 0;
		gbc_textFieldSign.gridy = 7;
		panel_7.add(textFieldSign, gbc_textFieldSign);
		textFieldSign.setColumns(10);
		
		
		
		JPanel panel_8 = new JPanel();
		panel_8.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel.add(panel_8, BorderLayout.SOUTH);
		panel_8.setLayout(new GridLayout(0, 1, 0, 0));
		
		JLabel lblSecretKey = new JLabel("Secret Key :");
		panel_8.add(lblSecretKey);
		
		textFieldSecretKey = new JTextField();
		panel_8.add(textFieldSecretKey);
		textFieldSecretKey.setHorizontalAlignment(SwingConstants.LEFT);
		textFieldSecretKey.setColumns(50);
		
		
		chckbxSameAsPara = new JCheckBox("Add secret key as a parameter, to sort with parameters");
		panel_8.add(chckbxSameAsPara);
		chckbxSameAsPara.setSelected(true);
		buttonGroup.add(chckbxSameAsPara);
		
		chckbxAppendToEnd = new JCheckBox("Append to the end of sorted Parameters(should contain connect string, such as & :)");
		panel_8.add(chckbxAppendToEnd);
		buttonGroup.add(chckbxAppendToEnd);
		
		JLabel lblNewLabel_1 = new JLabel("Para Black List : ");
		panel_8.add(lblNewLabel_1);
		
		textFieldBlackList = new JTextField();
		panel_8.add(textFieldBlackList);
		textFieldBlackList.setColumns(50);
		
		
		
		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panel_2, BorderLayout.EAST);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		textAreaFinalString = new JTextArea();
		textAreaFinalString.setLineWrap(true);
		textAreaFinalString.setColumns(20);
		textAreaFinalString.setRows(20);
		panel_2.add(textAreaFinalString, BorderLayout.WEST);
		
		textAreaSign = new JTextArea();
		textAreaSign.setLineWrap(true);
		textAreaSign.setColumns(20);
		panel_2.add(textAreaSign, BorderLayout.EAST);
		
		JPanel panel_10 = new JPanel();
		panel_2.add(panel_10, BorderLayout.NORTH);
		GridBagLayout gbl_panel_10 = new GridBagLayout();
		gbl_panel_10.columnWidths = new int[]{108, 43, 109, 0};
		gbl_panel_10.rowHeights = new int[]{23, 0, 0, 0, 0};
		gbl_panel_10.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_10.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_10.setLayout(gbl_panel_10);
		
		JLabel lblNewLabel_2 = new JLabel("Chose Sign Method:");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 0;
		panel_10.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		chckbxMD5 = new JCheckBox("MD5");
		chckbxMD5.setSelected(true);
		GridBagConstraints gbc_chckbxMD5 = new GridBagConstraints();
		gbc_chckbxMD5.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxMD5.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxMD5.gridx = 0;
		gbc_chckbxMD5.gridy = 1;
		panel_10.add(chckbxMD5, gbc_chckbxMD5);
		
		chckbxNewCheckBox_3 = new JCheckBox("To be Continue");
		chckbxNewCheckBox_3.setEnabled(false);
		GridBagConstraints gbc_chckbxNewCheckBox_3 = new GridBagConstraints();
		gbc_chckbxNewCheckBox_3.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxNewCheckBox_3.gridx = 1;
		gbc_chckbxNewCheckBox_3.gridy = 1;
		panel_10.add(chckbxNewCheckBox_3, gbc_chckbxNewCheckBox_3);
		
		JPanel panel_11 = new JPanel();
		panel_2.add(panel_11, BorderLayout.CENTER);
		
		JButton btnSign = new JButton("Sign");
		btnSign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				CMD5 getMD5 = new CMD5();
				String sign = getMD5.GetMD5Code(textAreaFinalString.getText());
				textAreaSign.setText(sign);
			}
		});
		panel_11.add(btnSign);
	}
	
	
    
    
    //���ִ�ͼ�������ߴ����ݰ���ȡ��������ȡ���õĺ�����--start
    
	public int checkEnabledFor(){
		//get values that should enable this extender for which Component.
		int status = 0;
		if (chckbxIntruder.isSelected()){
			status +=32;
		}
		if(chckbxProxy.isSelected()){
			status += 4;
		}
		if(chckbxRepeater.isSelected()){
			status += 64;
		}
		if(chckbxScanner.isSelected()){
			status += 16;
		}
		return status;
	}
	
	
	public void getSecKeyConfig() {
		if (secretKey != null && secretKey != ""){
			secretKey = textFieldSecretKey.getText();
		}
		if(chckbxAppendToEnd.isSelected()){
			howDealKey = "appendToEnd";
		}
		else if (chckbxSameAsPara.isSelected()) {
			howDealKey = "sameAsPara";
		}
	}
	
	public void getSortConfig() {
		try {
			sortedColumn = table.getRowSorter().getSortKeys().get(0).getColumn();
			//System.out.println(sortedColumn);
			sortedMethod = table.getRowSorter().getSortKeys().get(0).getSortOrder();
			//System.out.println(sortedMethod); //ASCENDING   DESCENDING
		} catch (Exception e) {
			sortedColumn = -1; //û�е����ͷ��������
			sortedMethod = null;
		}
	}
	
	
	public Map<String, String> getPara(IRequestInfo analyzeRequest){
    	List<IParameter> paras = analyzeRequest.getParameters();//��body��json��ʽ��ʱ���������Ҳ����������ȡ����ֵ�ԣ�ţ��������PARAM_JSON�ȸ�ʽ����ͨ��updateParameter���������¡�
    	Map<String,String> paraMap = new HashMap<String,String>();
    	for (IParameter para:paras){
    		if (!getBlackList().contains(para.getName())){
    			paraMap.put(para.getName(), para.getValue());
    		}
    	}
    	return paraMap ;
	}
	
	public byte getSignParaType(IRequestInfo analyzeRequest){
		List<IParameter> paras = analyzeRequest.getParameters();//��body��json��ʽ��ʱ���������Ҳ����������ȡ����ֵ�ԣ�ţ��������PARAM_JSON�ȸ�ʽ����ͨ��updateParameter���������¡�
		byte signParaType = -1;
		for (IParameter para:paras){
    		if (para.getName().equals(signPara)){
    			signParaType = para.getType();
    			
    		}
    	}
		return signParaType;
	}
	
	public Map<String, String> getParaFromTable(){
    	Map<String, String> tableParas = new HashMap<String, String>();
    	for (int i=0; i<table.getRowCount();i++){
    		//System.out.println(table.getRowCount());
    		String key = table.getValueAt(i, 0).toString();
    		//System.out.println(key);
    		String value = table.getValueAt(i, 1).toString();
    		//System.out.println(value);
    		if (!getBlackList().contains(key)){
    			tableParas.put(key, value);
    		}
    	}
    	System.out.println(tableParas);
    	return tableParas;
	}
	
	
	public String getHost(IRequestInfo analyzeRequest){
    	List<String> headers = analyzeRequest.getHeaders();
    	String domain = "";
    	for(String item:headers){
    		if (item.toLowerCase().contains("host")){
    			domain = new String(item.substring(6));
    		}
    	}
    	return domain ;
	}
	
	public List<String> getBlackList() {
		return Arrays.asList(textFieldBlackList.getText().split(" "));
	}
	
	
	public String getHostFromUI(){
    	String domain = "";
    	domain = textFieldDomain.getText();
    	return domain ;
	}
	//���ִ�ͼ�������ߴ����ݰ���ȡ��������ȡ���õĺ�����--end
	
	
	
	
	
	//��ϳ�signǰ���ַ�����
	public String combineString(Map<String, String> paraMap) {
		getSecKeyConfig();
		getSortConfig();
		String finalString = "";
		
		
		if (howDealKey.equals("sameAsPara")){
			secretKey = textFieldSecretKey.getText();
			if(secretKey.contains("=") & secretKey.split("=").length==2){
				paraMap.put(secretKey.split("=")[0], secretKey.split("=")[1]);
			}
		}
		
		if (sortedColumn == -1){//δ��������
			for(Map.Entry<String,String>para:paraMap.entrySet()){
				if (!finalString.equals("")){
					finalString += "&";
				}
				finalString += para.getKey()+"="+para.getValue();
			}
		}else if(sortedColumn == 0) {
			if (sortedMethod.toString() == "ASCENDING"){
				finalString = burp.CMapSort.combineMapEntry(burp.CMapSort.sortMapByKey(paraMap,"ASCENDING"), "&");
			}else if (sortedMethod.toString() == "DESCENDING") {
				finalString = burp.CMapSort.combineMapEntry(burp.CMapSort.sortMapByKey(paraMap,"DESCENDING"), "&");
			}
		}
		else if (sortedColumn == 1) {
			if (sortedMethod.toString() == "ASCENDING"){
				finalString = burp.CMapSort.combineMapEntry(burp.CMapSort.sortMapByValue(paraMap,"ASCENDING"), "&");
			}else if (sortedMethod.toString() == "DESCENDING") {
				finalString = burp.CMapSort.combineMapEntry(burp.CMapSort.sortMapByValue(paraMap,"DESCENDING"), "&");
			}
		}
		
		
		if (howDealKey.equals("appendToEnd")){
			secretKey = textFieldSecretKey.getText();
			finalString += secretKey;
		}
		return finalString;
	}
	
	
	
	
	
	//�����Ǹ���burp����ķ��� --start
    
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.CGUI();
          BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); //�����BurpExtender.thisʵ����ָITab����Ҳ����getUiComponent()�е�contentPane.���������CGUI()������ʼ����
          //������ﱨjava.lang.NullPointerException: Component cannot be null ������Ҫ�Ų�contentPane�ĳ�ʼ���Ƿ���ȷ��
        }
      });
    }
    
    
    
    //ITab����ʵ�ֵ���������
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("ReSign");
	}
	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return this.contentPane;
	}
	//ITab����ʵ�ֵ���������
	
	
	
	//IContextMenuFactory ����ʵ�ֵķ���
	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation)
	{ //��Ҫ��ǩ��ע�ᣡ��callbacks.registerContextMenuFactory(this);
	    IHttpRequestResponse[] messages = invocation.getSelectedMessages();
	    List<JMenuItem> list = new ArrayList<JMenuItem>();
	    if((messages != null) && (messages.length > 0))
	    {
	        //this.callbacks.printOutput("Messages in array: " + messages.length);
	        
	        //final IHttpService service = messages[0].getHttpService();
	    	final byte[] sentRequestBytes = messages[0].getRequest();
	    	IRequestInfo analyzeRequest = helpers.analyzeRequest(sentRequestBytes);
	    	
	        JMenuItem menuItem = new JMenuItem("Send to ReSign");
	        menuItem.addActionListener(new ActionListener()
	        {
	          public void actionPerformed(ActionEvent e)
	          {
	            try
	            {
	            	textFieldDomain.setText(getHost(analyzeRequest));
	            	
	            	DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
	            	tableModel.setRowCount(0);//Ϊ�����֮ǰ������
	            	
	            	Map<String,String> paraMap = getPara(analyzeRequest);
	            	for(String key:paraMap.keySet()){
	            		tableModel.addRow(new Object[]{key,paraMap.get(key)});
	            	}
	            }
	            catch (Exception e1)
	            {
	                BurpExtender.this.callbacks.printError(e1.getMessage());
	            }
	          }
	        });
	        list.add(menuItem);
	    }
	    return list;
	}
	//����burp����ķ��� --end
	
}