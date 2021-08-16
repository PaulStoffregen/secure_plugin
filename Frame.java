/*
 * Teensy 4 Security plugin adapted from Arduino's WiFi101 plugin
 * Portions are copyright 2021 Paul Stoffregen (paul@pjrc.com)
 * All code in this file, regardless of whether written by PJRC,
 * or Arduino may be used by the license terms below.
 *
 *   Most of the createGUI() function is copied from Arduino
 *   The rest of this file is Paul's original work
 *
 * This file is part of WiFi101 Updater Arduino-IDE Plugin.
 * Copyright 2016 Arduino LLC (http://www.arduino.cc/)
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */
package cc.arduino.plugins.T4Security;

import java.io.File;
import processing.app.helpers.*;
import cc.arduino.files.DeleteFilesOnShutdown;
import java.util.*;
import java.io.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import processing.app.Theme;
import processing.app.Base;
import processing.app.BaseNoGui;

public class Frame extends JFrame {
	private JPanel contentPane;
	private JPanel panel_1;
	private JPanel panel_2;
	private JButton keygenButton;
	private JButton fusesButton;
	private JButton verifyButton;
	private JButton lockButton;
	final File keyfile = new File(BaseNoGui.getSketchbookFolder(), "key.pem");
	final String teensy_secure_command = new String(BaseNoGui.getHardwarePath() +
		File.separator + "tools" + File.separator + "teensy_secure");

	public Frame() {
		createGUI();
		checkKeyFile(); // enables buttons if key is good
		pack(); // automatically sizes window
		Base.registerWindowCloseKeys(getRootPane(), e -> {
			setVisible(false);
		});
		Base.setIcon(this);
	}

	private boolean checkKeyFile() {
		boolean state = false;
		if (keyfile.isFile() && keyfile.canRead()) {
			String s = run("keycheck");
			if (s != null && s.contains("Key is valid")) state = true;
		}
		fusesButton.setEnabled(state);
		verifyButton.setEnabled(state);
		lockButton.setEnabled(state);
		return state;
	}

	private void keygen() {
		System.out.println("keygen");
		if (checkKeyFile()) {
			// TODO: check if key.pem exists, dialog to confirm new key
		}
		run("keygen");
		checkKeyFile();
	}

	private void fuses() {
		if (checkKeyFile()) {
			try {
				File newFile = createINO("FuseWrite");
				File dataFile = new File(newFile.getParentFile(), "testdata.ino");
				dataFile.createNewFile();
				String sketch = run("fuseino");
				String data = run("testdataino");
				if (sketch != null && data != null) {
					write_file(newFile, sketch);
					write_file(dataFile, data);
					Base.INSTANCE.handleOpen(newFile);
				} else {
					System.err.println(
						"Sorry, couldn't generate FuseWrite sketch");
				}
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		} else {
			System.err.println("Error: Can't access key.pem");
		}
	}

	private void verify() {
		if (checkKeyFile()) {
			try {
				File newFile = createINO("VerifySecure");
				String sketch = run("verifyino");
				if (sketch != null) {
					write_file(newFile, sketch);
					Base.INSTANCE.handleOpen(newFile);
				} else {
					System.err.println(
						"Sorry, couldn't generate VerifySecure sketch");
				}
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		} else {
			System.err.println("Error: Can't access key.pem");
		}
	}

	private void write_file(File f, String data) {
		try {
			OutputStream out = new FileOutputStream(f);
			out.write(data.getBytes("UTF8"));
			out.close();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	private void lock() {
		if (checkKeyFile()) {
			try {
				File newFile = createINO("LockSecureMode");
				String sketch = run("lockino");
				if (sketch != null) {
					write_file(newFile, sketch);
					Base.INSTANCE.handleOpen(newFile);
				} else {
					System.err.println(
						"Sorry, couldn't generate FuseWrite sketch");
				}
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		} else {
			System.err.println("Error: Can't access key.pem");
		}
	}

	private String run(String operation) {
		List<String> cmdline = new LinkedList<String>();
		cmdline.add(teensy_secure_command);
		cmdline.add(operation);
		cmdline.add(keyfile.getAbsolutePath());
		return run_program(cmdline);
	}

	private String run_program(List<String> cmdline) {
		try {
			ProcessBuilder pb = new ProcessBuilder(cmdline);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			Process program = pb.start();
			InputStreamReader in = new InputStreamReader(program.getInputStream());
			StringBuilder output = new StringBuilder();
			char[] buffer = new char[16384];
			while (true) {
				int n = in.read(buffer, 0, 16384);
				if (n < 0) break;
				if (n > 0) output.append(buffer, 0, n);
			}
			int ret = program.waitFor();
			if (ret == 0) return output.toString(); // success, stdout as String
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		return null; // any error
	}


	private File createINO(String name) throws IOException {
		String tmpName = "temp" + new Random().nextInt(Integer.MAX_VALUE);
		File tmpFolder = FileUtils.createTempFolder(tmpName);
		DeleteFilesOnShutdown.add(tmpFolder);
		File newDir = new File(tmpFolder, name);
		File newFile = new File(newDir, name + ".ino");
		if (!newDir.mkdirs() || !newFile.createNewFile()) {
			System.err.println("Unable to create " + newFile);
		}
		System.out.println("created " + newFile);
		return newFile;
	}

	private void createGUI() {
		final int scale = Theme.getScale();
		// control JLabel width: https://stackoverflow.com/questions/2420742
		String s1 = "<html><div WIDTH=" + (420 * scale / 100) + ">";
		String s2 = "</div></html>";

		setTitle("Teensy 4 Security");
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{0, 0};
		gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_contentPane.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{1.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);

		panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "1. Encryption Setup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(5, 5, 5, 5);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 0;
		contentPane.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);

		JLabel textKeygen = new JLabel();
		textKeygen.setText(s1+"Create a new encryption key.  The key will be " +
			"written at the path below.  " +
			"<b>Keep this file secret</b>.  Anyone who obtains key.pem " +
			"could decrypt your code.  " +
			"<b>Make backups</b>, as no way exists to recover this " +
			"file." + s2);
		textKeygen.setOpaque(false);
		GridBagConstraints gbc_textKeygen = new GridBagConstraints();
		gbc_textKeygen.fill = GridBagConstraints.HORIZONTAL;
		gbc_textKeygen.gridwidth = 2;
		gbc_textKeygen.insets = new Insets(5, 12, 0, 5);
		gbc_textKeygen.gridx = 0;
		gbc_textKeygen.gridy = 0;
		panel_1.add(textKeygen, gbc_textKeygen);

		keygenButton = new JButton("Generate Key");
		keygenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keygen();
			}
		});

		GridBagConstraints gbc_keygenButton = new GridBagConstraints();
		gbc_keygenButton.insets = new Insets(5, 5, 10, 5);
		gbc_keygenButton.gridx = 0;
		gbc_keygenButton.gridy = 1;
		panel_1.add(keygenButton, gbc_keygenButton);

		JLabel textKeypath = new JLabel();
		textKeypath.setText(s1+keyfile+s2);
		textKeypath.setOpaque(false);
		GridBagConstraints gbc_textKeypath = new GridBagConstraints();
		gbc_textKeypath.fill = GridBagConstraints.HORIZONTAL;
		gbc_textKeypath.gridwidth = 2;
		gbc_textKeypath.insets = new Insets(5, 28, 10, 20);
		gbc_textKeypath.gridx = 0;
		gbc_textKeypath.gridy = 3;
		panel_1.add(textKeypath, gbc_textKeypath);



		JLabel textCompileInfo = new JLabel();
		textCompileInfo.setText(s1+"Normal code is stored in a \".HEX\" file " +
			"and encrypted code is stored in an \".EHEX\" file. " +
			"Both are created with every compile " + 
			"when key.pem exists at this path."+s2);
		textCompileInfo.setOpaque(false);
		GridBagConstraints gbc_textCompileInfo = new GridBagConstraints();
		gbc_textCompileInfo.fill = GridBagConstraints.HORIZONTAL;
		gbc_textCompileInfo.gridwidth = 2;
		gbc_textCompileInfo.insets = new Insets(5, 12, 10, 20);
		gbc_textCompileInfo.gridx = 0;
		gbc_textCompileInfo.gridy = 4;
		panel_1.add(textCompileInfo, gbc_textCompileInfo);

		panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(null, "2. Teensy Hardware Setup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(5, 5, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		contentPane.add(panel_2, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_2.setLayout(gbl_panel);

		JLabel textFuses = new JLabel();
		textFuses.setText(s1 + "Write your encryption key to Teensy's " +
			"permanent fuse memory. " +
			"After the key is written, Teensy can run both normal and " +
			"encrypted programs." + s2);
		textFuses.setOpaque(false);
		GridBagConstraints gbc_textFuses = new GridBagConstraints();
		gbc_textFuses.insets = new Insets(5, 12, 0, 5);
		gbc_textFuses.fill = GridBagConstraints.BOTH;
		gbc_textFuses.gridx = 0;
		gbc_textFuses.gridy = 1;
		panel_2.add(textFuses, gbc_textFuses);

		fusesButton = new JButton("Fuse Write Sketch");
		fusesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fuses();
			}
		});

		GridBagConstraints gbc_fusesButton = new GridBagConstraints();
		gbc_fusesButton.insets = new Insets(5, 5, 10, 5);
		gbc_fusesButton.gridx = 0;
		gbc_fusesButton.gridy = 2;
		panel_2.add(fusesButton, gbc_fusesButton);

		JLabel textVerify = new JLabel();
		textVerify.setText(s1+"Verify an encrypted program runs properly."+s2);
		textVerify.setOpaque(false);
		GridBagConstraints gbc_textVerify = new GridBagConstraints();
		gbc_textVerify.insets = new Insets(5, 12, 0, 5);
		gbc_textVerify.fill = GridBagConstraints.BOTH;
		gbc_textVerify.gridx = 0;
		gbc_textVerify.gridy = 3;
		panel_2.add(textVerify, gbc_textVerify);

		verifyButton = new JButton("Verify Sketch");
		verifyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verify();
			}
		});

		GridBagConstraints gbc_verifyButton = new GridBagConstraints();
		gbc_verifyButton.insets = new Insets(5, 5, 10, 5);
		gbc_verifyButton.gridx = 0;
		gbc_verifyButton.gridy = 4;
		panel_2.add(verifyButton, gbc_verifyButton);

		JLabel textLock = new JLabel();
		textLock.setText(s1+"Permanently lock secure mode.  Once locked, " +
			"Teensy will only be able to run programs encrypted by " +
			"your key, and JTAG access is disabled.  This step is " +
			"required for full security." + s2);
		textLock.setOpaque(false);
		GridBagConstraints gbc_textLock = new GridBagConstraints();
		gbc_textLock.insets = new Insets(5, 12, 0, 5);
		gbc_textLock.fill = GridBagConstraints.BOTH;
		gbc_textLock.gridx = 0;
		gbc_textLock.gridy = 5;
		panel_2.add(textLock, gbc_textLock);

		lockButton = new JButton("Lock Security Sketch");
		lockButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lock();
			}
		});

		GridBagConstraints gbc_lockButton = new GridBagConstraints();
		gbc_lockButton.insets = new Insets(5, 5, 10, 5);
		gbc_lockButton.gridx = 0;
		gbc_lockButton.gridy = 6;
		panel_2.add(lockButton, gbc_lockButton);
	}
}
