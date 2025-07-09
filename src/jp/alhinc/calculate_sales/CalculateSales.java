package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		//listFilesメソッドにて指定したディレクトリに含まれる対象ファイル情報を格納
		//listFiles() メソッド：File オブジェクトが表すディレクトリ内のすべてのファイルとサブディレクトリを抽象パス名として返す
		//listFiles() メソッドの戻り値は、 File オブジェクトの配列 (File[]) になる
		File[] files = new File(args[0]).listFiles();

		//対象の売上ファイルを格納するためのrcdFilesリストを宣言
		List<File> rcdFiles = new ArrayList<>();

		//指定したパスに存在するFile オブジェクトの総数、つまりそのパス先に存在するファイルの数だけ処理を繰り返す
		for(int i = 0; i < files.length ; i++) {
			//files[i].getName()でファイル名取得できる
			String fileName = files[i].getName();

			//.isFile→ファイルかどうかを調べる
			//拡張子rcdかつファイル名が数字8桁のファイル名だったら、List(ArrayList)に追加する
			if(files[i].isFile() && fileName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//支店定義ファイル読み込み
		BufferedReader br = null;

		//rcdFilesに複数の売上ファイルを格納しているので、その数だけ処理を繰り返す
		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				//売上ファイル「rcdFiles」を読み込む
				br = new BufferedReader(new FileReader(rcdFiles.get(i)));

				//売上金額を入れるためのリスト「fileContents」を作成する→ここの理解が抜けてた
				ArrayList<String> fileContents = new ArrayList<>();
				//rcdFilesの1行目に支店コード、2行目には売上金額が入っている
				//最後の行まで読み込み、fileContentsに要素を1つずつ追加していく
				String line = "";
				while((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				//branchCode変数に、fileContentsリストの1つめの要素、つまり支店コードを入れる
				//branchCodeは、後でMapの支店コードをKeyにValueを取り出す際に使用する
				String branchCode = fileContents.get(0);

				//ファイルから読み込んだ情報は一律で文字列として扱われるので、Long型に変換する
				//long型のfileSale変数に、fileContentsリストの2つめの要素、つまり売上金額を入れる
				long fileSale = Long.parseLong(fileContents.get(1));

				//読み込んだ売上金額を加算する
				//Map「branchSales」のKey(支店コード)を渡してバリュー(売上金額)を取り出し、longに変換した売上金額(foleSale)を加算する
				Long saleAmount = branchSales.get(branchCode) + fileSale;

				//加算した売上金額をMap「branchSales」に追加
				branchSales.put(branchCode, saleAmount);

			} catch(IOException e)  {
				System.out.println(UNKNOWN_ERROR);
				return;
			}finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}

		}


		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			String[] items;

			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// 処理内容1-2
				//splitにて文字列をカンマで分割
				items = line.split(",");

				//Map「branchNames」に支店コードと支店名を追加する
				//支店コードは配列の0番目、支店名は配列の1番目に入っている
				branchNames.put(items[0], items[1]);

				//Map「 branchSales」に支店コードと売上金額を追加する
				//売上金額はまだ不明だがシステム上前日の売上金額を繰り越さないため、「0」円で追加
				//売上金額はlong型　単純に0と入れるだけではint型で扱われてしまう
				//Longで固定値を追加する場合は、明示的にLongの整数と示す(=数字に大文字Lをつける)orキャストしてもいい((データ型)式みたいな書き方でも可)
				branchSales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)

		return true;
	}

}
